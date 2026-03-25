package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.OrderItemRequestDTO;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.events.OrderStatusUpdatedEvent;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final CartRepository cartRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            FoodItemRepository foodItemRepository,CartRepository cartRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
        this.cartRepository=cartRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail) {

        log.info("Placing order for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No food items selected");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        //  STEP 1: MERGE DUPLICATE ITEMS
        Map<Long, Integer> mergedItems = new HashMap<>();

        for (OrderItemRequestDTO item : request.getItems()) {
            mergedItems.merge(
                    item.getFoodItemId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        //  STEP 2: CREATE ORDER ITEMS
        List<OrderItem> orderItems = mergedItems.entrySet()
                .stream()
                .map(entry -> {

                    Long foodId = entry.getKey();
                    Integer quantity = entry.getValue();

                    FoodItem food = foodItemRepository.findById(foodId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Food item not found with id: " + foodId
                            ));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setFoodItem(food);
                    orderItem.setQuantity(quantity); //  merged quantity
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

        Order saved = orderRepository.save(order);

        OrderResponseDTO response = OrderMapper.toDTO(saved);

        eventPublisher.publishEvent(new OrderCreatedEvent(response));

        return response;
    }

    @Override
    public List<OrderResponseDTO> getUserOrder(String userEmail) {

        log.info("Fetching orders for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("User not found while fetching orders: {}", userEmail);
                    return new UserNotFoundException("User not found");
                });

        List<OrderResponseDTO> orders = orderRepository.findByUser(user)
                .stream()
                .map(OrderMapper::toDTO)
                .toList();

        log.info("Found {} orders for user {}", orders.size(), userEmail);

        return orders;
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {

        log.info("Fetching all orders");

        List<OrderResponseDTO> orders = orderRepository.findAll()
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

    @Override
    public List<OrderResponseDTO> getPendingOrders() {

        log.info("Fetching pending orders");

        List<OrderResponseDTO> orders = orderRepository.findByStatus(OrderStatus.PENDING)
                .stream()
                .map(OrderMapper::toDTO)
                .toList();

        log.info("Pending orders count: {}", orders.size());

        return orders;
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

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        log.info("Order {} status updated to {}", orderId, newStatus);

        OrderResponseDTO response = OrderMapper.toDTO(order);

        //  EVENT TRIGGER
        log.info("Publishing order status update event for orderId: {}", orderId);
        eventPublisher.publishEvent(new  OrderStatusUpdatedEvent(response));

        return response;
    }

    @Override
    public byte[] generateInvoice(Long orderId) {

        log.info("Generating invoice for order ID: {}", orderId);

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

        //  INVOICE GENERATION LOGIC (SIMULATED)4
        String invoiceContent = "Invoice for Order ID: " + order.getId() + "\n" +
                "Customer: " + order.getUser().getName() + "\n" +
                "Total Amount: $" + order.getTotalAmount() + "\n" +
                "Status: " + order.getStatus() + "\n" +
                "Items:\n";

        for (OrderItem item : order.getOrderItems()) {
            invoiceContent += "- " + item.getFoodItem().getName() +
                    " x" + item.getQuantity() +
                    " @ $" + item.getFoodItem().getPrice() + "\n";
        }

        log.info("Invoice content generated for order ID {}: \n{}", orderId, invoiceContent);

        return invoiceContent.getBytes();
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

    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus next) {

        log.info("Validating status transition from {} to {}", current, next);

        switch (current) {

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