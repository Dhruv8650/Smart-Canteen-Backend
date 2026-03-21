package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.OrderItemRequestDTO;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.*;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            FoodItemRepository foodItemRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
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
        eventPublisher.publishEvent(new OrderCreatedEvent(response));

        return response;
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