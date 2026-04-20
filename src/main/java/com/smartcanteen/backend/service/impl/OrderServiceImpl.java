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
import com.smartcanteen.backend.service.CartService;
import com.smartcanteen.backend.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final CartService cartService;

    private record ValidatedOrderLine(FoodItem foodItem, int quantity) {}

    private record OrderDraft(User user,
                              List<ValidatedOrderLine> lines,
                              OrderType orderType,
                              BigDecimal totalAmount) {}


    @Override
    @Transactional
    public OrderResponseDTO approvePayment(Long orderId){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if(order.getStatus() != OrderStatus.PAYMENT_PENDING){
            throw new IllegalStateException("Order is not waiting for payment");
        }

        if(order.getPaymentMethod() != PaymentMethod.CASH){
            throw new IllegalStateException("Only cash orders can be approved");
        }
        // Null Safety
        if (order.getOrderType() == null) {
            log.warn("OrderType is null for orderId: {}. Defaulting to PREPARED", orderId);
            order.setOrderType(OrderType.PREPARED);
        }

        if (OrderType.READYMADE.equals(order.getOrderType()))  {
            order.setStatus(OrderStatus.READY);
            order.setReadyAt(LocalDateTime.now());
        } else {
            order.setStatus(OrderStatus.PENDING);
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);

        Order updated = orderRepository.save(order);

        OrderResponseDTO response = OrderMapper.toDTO(updated);

        eventPublisher.publishEvent(new OrderStatusUpdatedEvent(response));

        log.info("Payment approved for orderId: {}", orderId);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail) {
        return placeOrderInternal(request, userEmail, OrderSource.USER, null, null, null, null, null);
    }

    @Override
    @Transactional
    public OrderResponseDTO placePosOrder(OrderRequestDTO request, String adminEmail) {
        return placeOrderInternal(
                request,
                adminEmail,
                OrderSource.POS,
                null,
                null,
                null,
                PaymentStatus.SUCCESS,
                null
        );
    }

    @Override
    public BigDecimal calculateOrderAmount(OrderRequestDTO request, String userEmail) {
        return prepareOrderDraft(request, userEmail, true, null).totalAmount();
    }

    @Override
    @Transactional
    public OrderResponseDTO placeVerifiedOnlineOrder(OrderRequestDTO request,
                                                     String userEmail,
                                                     String paymentOrderId,
                                                     String paymentId,
                                                     String paymentSignature,
                                                     BigDecimal paidAmount) {
        if (request.getPaymentMethod() == null || request.getPaymentMethod() == PaymentMethod.CASH) {
            throw new IllegalArgumentException("Verified online orders require UPI or CARD");
        }

        return placeOrderInternal(
                request,
                userEmail,
                OrderSource.USER,
                paymentOrderId,
                paymentId,
                paymentSignature,
                PaymentStatus.SUCCESS,
                paidAmount
        );
    }

    private OrderResponseDTO placeOrderInternal(
            OrderRequestDTO request,
            String userEmail,
            OrderSource source,
            String paymentOrderId,
            String paymentId,
            String paymentSignature,
            PaymentStatus paymentStatusOverride,
            BigDecimal totalAmountOverride
    ) {
        if (source == OrderSource.USER &&
                request.getPaymentMethod() != PaymentMethod.CASH &&
                paymentStatusOverride != PaymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Use /payments/create-order for online payments");
        }

        boolean enforceRealtimeChecks = !(source == OrderSource.USER &&
                paymentStatusOverride == PaymentStatus.SUCCESS &&
                request.getPaymentMethod() != PaymentMethod.CASH);

        OrderDraft draft = prepareOrderDraft(request, userEmail, enforceRealtimeChecks, totalAmountOverride);

        return persistOrder(
                request,
                draft,
                source,
                paymentOrderId,
                paymentId,
                paymentSignature,
                paymentStatusOverride
        );
    }

    private OrderDraft prepareOrderDraft(OrderRequestDTO request,
                                         String userEmail,
                                         boolean enforceRealtimeChecks,
                                         BigDecimal totalAmountOverride) {
        if (enforceRealtimeChecks && !canteenService.canAcceptOrders()) {
            log.warn("Order blocked - canteen is closed for user: {}", userEmail);
            throw new RuntimeException("Canteen is not accepting orders");
        }

        log.info("Placing order for user: {}", userEmail);

        // FETCH USER
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No food items selected");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        // MERGE DUPLICATE ITEMS
        Map<Long, Integer> mergedItems = new HashMap<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            mergedItems.merge(
                    item.getFoodItemId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        List<ValidatedOrderLine> lines = mergedItems.entrySet()
                .stream()
                .map(entry -> {

                    Long foodId = entry.getKey();
                    Integer quantity = entry.getValue();

                    FoodItem food = foodItemRepository.findById(foodId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Food item not found with id: " + foodId
                            ));

                    if (quantity == null || quantity <= 0) {
                        throw new IllegalArgumentException("Invalid quantity for item: " + foodId);
                    }

                    if (enforceRealtimeChecks && !food.isAvailable()) {
                        throw new IllegalStateException("Food item not available: " + food.getName());
                    }

                    // Max limit only for prepared items
                    if (enforceRealtimeChecks && Boolean.TRUE.equals(food.getIsPreparedItem())) {

                        if (food.getMaxPerOrder() != null &&
                                quantity > food.getMaxPerOrder()) {

                            throw new MaxOrderLimitExceededException(
                                    "You can only order " + food.getMaxPerOrder() + " " + food.getName()
                            );
                        }
                    }

                    return new ValidatedOrderLine(food, quantity);
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        // DETECT PREPARATION REQUIREMENT
        boolean requiresPreparation = lines.stream()
                .anyMatch(item ->
                        Boolean.TRUE.equals(item.foodItem().getIsPreparedItem())
                );

        OrderType orderType = requiresPreparation
                ? OrderType.PREPARED
                : OrderType.READYMADE;

        log.info("Order type: {}", orderType);

        BigDecimal total = totalAmountOverride != null
                ? totalAmountOverride
                : lines.stream()
                .map(item -> item.foodItem()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderDraft(user, lines, orderType, total);
    }

    private OrderResponseDTO persistOrder(OrderRequestDTO request,
                                          OrderDraft draft,
                                          OrderSource source,
                                          String paymentOrderId,
                                          String paymentId,
                                          String paymentSignature,
                                          PaymentStatus paymentStatusOverride) {
        Order order = new Order();
        order.setUser(draft.user());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setSource(source);
        order.setOrderType(draft.orderType());
        order.setTotalAmount(draft.totalAmount());
        order.setPaymentOrderId(paymentOrderId);
        order.setPaymentId(paymentId);
        order.setPaymentSignature(paymentSignature);
        order.setPaymentStatus(resolvePaymentStatus(request.getPaymentMethod(), source, paymentStatusOverride));

        List<OrderItem> orderItems = draft.lines().stream()
                .map(line -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setFoodItem(line.foodItem());
                    orderItem.setQuantity(line.quantity());
                    orderItem.setOrder(order);
                    return orderItem;
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        order.setOrderItems(orderItems);

        boolean isPosOrder = source == OrderSource.POS;

        if (isPosOrder) {

            if (draft.orderType() == OrderType.READYMADE) {
                order.setStatus(OrderStatus.READY);
                order.setReadyAt(LocalDateTime.now());
            } else {
                order.setStatus(OrderStatus.PENDING);
            }

        } else {

            if (draft.orderType() == OrderType.READYMADE) {

                if (request.getPaymentMethod() == PaymentMethod.CASH) {
                    order.setStatus(OrderStatus.PAYMENT_PENDING);
                } else {
                    LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
                    order.setStatus(OrderStatus.READY);
                    order.setReadyAt(nowUtc);
                    order.setPickupExpiry(nowUtc.plusMinutes(45));

                }

            } else {


                if (request.getPaymentMethod() == PaymentMethod.CASH) {
                    order.setStatus(OrderStatus.PAYMENT_PENDING);
                } else {
                    order.setStatus(OrderStatus.PENDING);
                }
            }
        }

        //  SAVE ORDER
        Order saved = orderRepository.save(order);

        //  FETCH WITH RELATIONS (SAFE)
        saved = orderRepository.findByIdWithItems(saved.getId())
                .orElseThrow(() -> new RuntimeException("Order not found after save"));

        //  QR GENERATION
        String baseCode = generatePickupCode(saved.getId());
        String payload = baseCode + "|" + saved.getId();
        String signature = qrSecurityUtil.generateSignature(payload);
        String finalCode = payload + "|" + signature;

        saved.setPickupCode(finalCode);
        saved = orderRepository.save(saved);

        //  CLEAR CART ONLY FOR USER ORDERS
        if (saved.getSource() == null || saved.getSource() == OrderSource.USER) {
            cartService.clearCart(draft.user());
            log.info("Cart cleared for user: {}", draft.user().getEmail());
        }



        log.info("Order saved with ID: {} and status: {}", saved.getId(), saved.getStatus());

        // MAP RESPONSE
        OrderResponseDTO response = OrderMapper.toDTO(saved);

        //  EVENT (WebSocket)
        eventPublisher.publishEvent(new OrderCreatedEvent(response));

        return response;
    }

    private PaymentStatus resolvePaymentStatus(PaymentMethod paymentMethod,
                                               OrderSource source,
                                               PaymentStatus paymentStatusOverride) {
        if (paymentStatusOverride != null) {
            return paymentStatusOverride;
        }

        if (source == OrderSource.POS) {
            return PaymentStatus.SUCCESS;
        }

        if (paymentMethod == PaymentMethod.CASH) {
            return PaymentStatus.INITIATED;
        }

        return PaymentStatus.SUCCESS;
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
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            order.setReadyAt(nowUtc);
            order.setPickupExpiry(nowUtc.plusMinutes(45));
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
    @Transactional
    public void cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String email = SecurityUtils.getCurrentUserEmail();
        boolean isAdmin = SecurityUtils.isAdmin();

        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You are not allowed to cancel this order");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed order");
        }

        order.setStatus(OrderStatus.CANCELLED);

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
    @Transactional
    public OrderResponseDTO verifyAndReturn(String code) {

        log.info("QR verify request received: {}", code);

        String[] parts = code.split("\\|");

        if (parts.length != 3) {
            log.warn("Invalid QR format: {}", code);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid QR format"
            );
        }

        String baseCode = parts[0];
        String orderIdStr = parts[1];
        String signature = parts[2];

        Long orderId;

        try {
            orderId = Long.parseLong(orderIdStr);
        } catch (Exception e) {
            log.warn("Invalid orderId in QR: {}", orderIdStr);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid QR data"
            );
        }

        String payload = baseCode + "|" + orderIdStr;

        //  VERIFY SIGNATURE
        if (!qrSecurityUtil.verify(payload, signature)) {
            log.warn("QR signature invalid for payload: {}", payload);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid QR (tampered)"
            );
        }

        Order order = orderRepository.findByPickupCodeWithDetails(code)
                .orElseThrow(() -> {
                    log.warn("QR not found in DB: {}", code);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Invalid QR code"
                    );
                });

        if (!order.getId().equals(orderId)) {
            log.warn("QR mismatch: expected {}, found {}", orderId, order.getId());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "QR mismatch"
            );
        }

        //  QR REUSE CHECK
        if (Boolean.TRUE.equals(order.getQrUsed()) ||
                order.getStatus() == OrderStatus.COMPLETED) {

            log.warn("QR already used for order {}", order.getId());

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "QR already used"
            );
        }

        // STRICT STATE VALIDATION
        if (order.getStatus() != OrderStatus.READY) {

            log.warn("Invalid state for verification: {}", order.getStatus());

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order not ready for pickup"
            );
        }


        // EXPIRY CHECK
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        if (order.getPickupExpiry() != null) {
            long diffSeconds = Duration.between(nowUtc, order.getPickupExpiry()).getSeconds();

            log.warn("QR expiry check -> orderId={}, nowUtc={}, expiryUtc={}, diffSeconds={}, status={}, qrUsed={}",
                    order.getId(),
                    nowUtc,
                    order.getPickupExpiry(),
                    diffSeconds,
                    order.getStatus(),
                    order.getQrUsed());

            if (nowUtc.isAfter(order.getPickupExpiry().plusSeconds(10))) {

                throw new ResponseStatusException(
                        HttpStatus.GONE,
                        "QR expired"
                );
            }
        }


        //  COMPLETE ORDER
        order.setStatus(OrderStatus.COMPLETED);
        order.setQrUsed(true);
        order.setQrUsedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        log.info("Order {} verified successfully", saved.getId());

        // 📡 WEBSOCKET EVENT
        OrderResponseDTO response = OrderMapper.toDTO(saved);

        eventPublisher.publishEvent(
                new OrderStatusUpdatedEvent(response)
        );

        return response;
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
