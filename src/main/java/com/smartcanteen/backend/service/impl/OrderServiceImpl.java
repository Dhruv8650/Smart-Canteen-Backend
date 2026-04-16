package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.OrderItemRequestDTO;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.events.OrderStatusUpdatedEvent;
import com.smartcanteen.backend.exception.MaxOrderLimitExceededException;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.QrSecurityUtil;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.CanteenService;
import com.smartcanteen.backend.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final CartRepository cartRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CanteenService canteenService;
    private final QrSecurityUtil qrSecurityUtil;


    @Override
    @Transactional
    public OrderResponseDTO approvePayment(Long orderId){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        //  Only PAYMENT_PENDING
        if(order.getStatus() != OrderStatus.PAYMENT_PENDING){
            throw new IllegalStateException("Order is not waiting for payment");
        }

        //  Only CASH
        if(order.getPaymentMethod() != PaymentMethod.CASH){
            throw new IllegalStateException("Only cash orders can be approved");
        }

        order.setStatus(OrderStatus.PENDING);

        Order updated = orderRepository.save(order);

        OrderResponseDTO response = OrderMapper.toDTO(updated);

        //  event
        eventPublisher.publishEvent(new OrderStatusUpdatedEvent(response));

        log.info("Payment approved for orderId: {}", orderId);

        return response; //
    }


    @Override
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail) {

        // CANTEEN CHECK
        if (!canteenService.canAcceptOrders()) {
            log.warn("Order blocked - canteen is closed for user: {}", userEmail);
            throw new RuntimeException("Canteen is not accepting orders");
        }

        log.info("Placing order for user: {}", userEmail);

        // 🔹 Fetch user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No food items selected");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        //  Create Order
        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(request.getPaymentMethod());

        //  PAYMENT LOGIC (CORE)
        if (request.getPaymentMethod() == PaymentMethod.CASH) {
            order.setStatus(OrderStatus.PAYMENT_PENDING);
        } else if (request.getPaymentMethod() == PaymentMethod.UPI) {
            // UPI (demo online payment)
            order.setStatus(OrderStatus.PENDING);
        } else {
            throw new IllegalStateException("Unsupported payment method");
        }

        //  STEP 1: MERGE DUPLICATE ITEMS
        Map<Long, Integer> mergedItems = new HashMap<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            mergedItems.merge(
                    item.getFoodItemId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        // 🔹 STEP 2: CREATE ORDER ITEMS
        List<OrderItem> orderItems = mergedItems.entrySet()
                .stream()
                .map(entry -> {

                    Long foodId = entry.getKey();
                    Integer quantity = entry.getValue();

                    FoodItem food = foodItemRepository.findById(foodId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Food item not found with id: " + foodId
                            ));

                    if (Boolean.TRUE.equals(food.getIsPreparedItem())) {

                        if (food.getMaxPerOrder() != null &&
                                quantity > food.getMaxPerOrder()) {

                            throw new MaxOrderLimitExceededException(
                                    "You can only order " + food.getMaxPerOrder() + " " + food.getName()
                            );
                        }
                    }

                    OrderItem orderItem = new OrderItem();
                    orderItem.setFoodItem(food);
                    orderItem.setQuantity(quantity);
                    orderItem.setOrder(order);

                    return orderItem;
                })
                .toList();

        order.setOrderItems(orderItems);

        //  STEP 3: TOTAL CALCULATION
        BigDecimal total = orderItems.stream()
                .map(item -> item.getFoodItem()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);


        //  SAVE ORDER
        Order saved = orderRepository.save(order);

        try {
            saved = orderRepository.findByIdWithItems(saved.getId())
                    .orElseThrow(() -> new RuntimeException("Order not found after save"));
        } catch (Exception e) {
            e.printStackTrace(); // 🔥 will reveal exact error
            throw e;
        }

        String baseCode = generatePickupCode(saved.getId());

        String payload = baseCode + "|" + saved.getId();

        String signature = qrSecurityUtil.generateSignature(payload);

        String finalCode = payload + "|" + signature;

        saved.setPickupCode(finalCode);

        log.info("Order saved with ID: {} and status: {}", saved.getId(), saved.getStatus());

        //  MAP TO DTO
        OrderResponseDTO response = OrderMapper.toDTO(saved);

        //  EVENT (WebSocket / realtime)
        eventPublisher.publishEvent(new OrderCreatedEvent(response));

        return response;
    }

    @Transactional
    @Override
    public OrderResponseDTO rejectOrder(Long orderId) {

        log.info("Rejecting order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        //  ROLE CHECK
        if (!SecurityUtils.isAdmin() && !SecurityUtils.isManager()) {
            throw new AccessDeniedException("Only admin or manager can reject orders");
        }

        //  Prevent invalid states
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot reject completed order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order already cancelled");
        }


        validateStatusTransition(order.getStatus(), OrderStatus.CANCELLED);

        order.setStatus(OrderStatus.CANCELLED);

        Order updated = orderRepository.save(order);

        OrderResponseDTO response = OrderMapper.toDTO(updated);

        //  REAL-TIME UPDATE
        eventPublisher.publishEvent(new OrderStatusUpdatedEvent(response));

        log.info("Order {} rejected successfully", orderId);

        return response;
    }

    @Override
    public List<OrderResponseDTO> getOrdersByStatuses(List<OrderStatus> statuses) {

        List<Order> orders = orderRepository.findByStatusesWithDetails(statuses);

        return orders.stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    public List<OrderResponseDTO> getUserOrder(String userEmail) {

        log.info("Fetching orders for user: {}", userEmail);

        return orderRepository.findOrdersByUserEmail(userEmail)
                .stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<OrderResponseDTO> getAllOrders() {

        log.info("Fetching all orders");

        List<OrderResponseDTO> orders = orderRepository.findAllWithDetails()
                .stream()
                .map(OrderMapper::toDTO)
                .toList();

        log.info("Total orders fetched: {}", orders.size());

        return orders;
    }

    @Override
    public OrderResponseDTO getOrderById(Long orderId) {

        log.info("Fetching order by ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isAdmin();

        //  CHECK
        if (currentUserEmail == null && !isAdmin) {
            throw new RuntimeException("User not authenticated");
        }

        //  ROLE + OWNERSHIP CHECK
        if (!isAdmin && !order.getUser().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Access denied");
        }

        return OrderMapper.toDTO(order);
    }

    @Transactional
    @Override
    public OrderResponseDTO updateOrderStatus(Long orderId,
                                              OrderStatus newStatus) {


        log.info("Updating order {} to status {}", orderId, newStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found: {}", orderId);
                    return new OrderNotFoundException("Order not found with id: " + orderId);
                });

        if (newStatus == OrderStatus.READY) {
            order.setReadyAt(LocalDateTime.now());
            order.setPickupExpiry(LocalDateTime.now().plusMinutes(20));
        }

        //  AUTH CHECK
        if (SecurityUtils.getCurrentUserRole() == null) {
            throw new IllegalStateException("User not authenticated");
        }

        log.info("User role {} attempting status update", SecurityUtils.getCurrentUserRole());

        //  ROLE-BASED VALIDATION
        if (SecurityUtils.isKitchen()) {

            if (newStatus != OrderStatus.PREPARING &&
                    newStatus != OrderStatus.READY) {
                throw new IllegalStateException("Kitchen can only set PREPARING or READY");
            }

        } else if (SecurityUtils.isManager()) {

            if (newStatus != OrderStatus.COMPLETED) {
                throw new IllegalStateException("Manager can only mark orders as COMPLETED");
            }

            if (order.getStatus() != OrderStatus.READY) {
                throw new IllegalStateException("Only READY orders can be completed");
            }

        } else {
            throw new IllegalStateException("Unauthorized role for updating order");
        }

        //  EXISTING VALIDATION
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        log.info("Order {} status updated to {}", orderId, newStatus);

        OrderResponseDTO response = OrderMapper.toDTO(order);

        //  EVENT
        eventPublisher.publishEvent(new OrderStatusUpdatedEvent(response));

        return response;
    }

    @Override
    @Transactional
    public void reorder(Long orderId) {

        log.info("Processing reorder for orderId: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found for reorder: {}", orderId);
                    return new OrderNotFoundException("Order not found");
                });

        String currentUserEmail = SecurityUtils.getCurrentUserEmail();

        if (!order.getUser().getEmail().equals(currentUserEmail)) {
            log.error("Unauthorized reorder attempt for orderId: {} by user: {}", orderId, currentUserEmail);
            throw new AccessDeniedException("Access denied");
        }

        User user = order.getUser();

        log.info("Fetching or creating cart for user: {}", user.getEmail());

        Cart cart = cartRepository.findByUser(user)
                .orElseGet(() -> {
                    log.info("No cart found. Creating new cart for user: {}", user.getEmail());
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        for (OrderItem item : order.getOrderItems()) {

            log.info("Processing item {} for reorder", item.getFoodItem().getId());

            CartItem existing = cart.getCartItems()
                    .stream()
                    .filter(ci -> ci.getFoodItem().getId().equals(item.getFoodItem().getId()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                log.info("Item already exists in cart. Updating quantity.");
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
            } else {
                log.info("Adding new item to cart");

                CartItem newItem = new CartItem();
                newItem.setFoodItem(item.getFoodItem());
                newItem.setQuantity(item.getQuantity());

                cart.addItem(newItem);
            }
        }

        cartRepository.save(cart);

        log.info("Reorder completed successfully for orderId: {}", orderId);
    }

    @Override
    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        OrderStatus status = order.getStatus();

        if (status == OrderStatus.PAYMENT_PENDING || status == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
        } else {
            throw new IllegalStateException("Cannot cancel at this stage");
        }

        orderRepository.save(order);
    }

    @Override
    public boolean hasActiveOrders() {

        return orderRepository.countActiveOrdersSmart(LocalDateTime.now()) > 0;
    }

    private String generatePickupCode(Long orderId) {
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORDER_" + orderId + "_" + random;
    }

    @Override
    public Order verifyAndReturn(String code) {

        String[] parts = code.split("\\|");

        if (parts.length != 3) {
            throw new RuntimeException("Invalid QR format");
        }

        String baseCode = parts[0];
        String orderId = parts[1];
        String signature = parts[2];

        String payload = baseCode + "|" + orderId;

        //  VERIFY SIGNATURE
        if (!qrSecurityUtil.verify(payload, signature)) {
            throw new RuntimeException("Invalid QR (tampered)");
        }

        Order order = orderRepository.findByPickupCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Order already collected");
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new RuntimeException("Order not ready yet");
        }

        boolean isLate = false;

        if (order.getPickupExpiry() != null &&
                LocalDateTime.now().isAfter(order.getPickupExpiry())) {

            isLate = true;
        }

        //  COMPLETE ORDER
        order.setStatus(OrderStatus.COMPLETED);

        Order saved = orderRepository.save(order);

        // WEBSOCKET EVENT
        OrderResponseDTO response = OrderMapper.toDTO(saved);

        eventPublisher.publishEvent(
                new OrderStatusUpdatedEvent(response)
        );

        if (isLate) {
            log.warn("Late pickup for order {}", order.getId());
        }

        return saved;
    }

    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus next) {

        log.info("Validating status transition from {} to {}", current, next);

        switch (current) {

            case PAYMENT_PENDING -> {
                if (next != OrderStatus.PENDING &&
                        next != OrderStatus.CANCELLED) {
                    throw new IllegalStateException("Invalid transition from PAYMENT_PENDING");
                }
            }

            case PENDING -> {
                if (next != OrderStatus.PREPARING &&
                        next != OrderStatus.CANCELLED) {
                    log.warn("Invalid transition from PENDING to {}", next);
                    throw new IllegalStateException("Invalid transition from PENDING");
                }
            }

            case PREPARING -> {
                if (next != OrderStatus.READY &&
                        next != OrderStatus.CANCELLED) {
                    log.warn("Invalid transition from PREPARING to {}", next);
                    throw new IllegalStateException("Invalid transition from PREPARING");
                }
            }

            case READY -> {
                if (next != OrderStatus.COMPLETED) {
                    log.warn("Invalid transition from READY to {}", next);
                    throw new IllegalStateException("Invalid transition from READY");
                }
            }

            default -> {
                log.error("Invalid order state: {}", current);
                throw new IllegalStateException("Order cannot be modified in current state");
            }
        }
    }


}