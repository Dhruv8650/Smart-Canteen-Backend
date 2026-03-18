package com.smartcanteen.backend.service.impl;

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
                .orElseThrow(() -> {
                    log.error("User not found: {}", userEmail);
                    return new UserNotFoundException("User not found");
                });

        if (request.getFoodItemIds().isEmpty()) {
            log.warn("Empty food item list for user: {}", userEmail);
            throw new IllegalArgumentException("No food items selected");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        log.info("Creating order object for user: {}", userEmail);

        List<OrderItem> orderItems = request.getFoodItemIds()
                .stream()
                .map(foodId -> {

                    FoodItem food = foodItemRepository.findById(foodId)
                            .orElseThrow(() -> {
                                log.error("Food item not found: {}", foodId);
                                return new RuntimeException("Food item not found with id: " + foodId);
                            });

                    log.info("Adding food item {} to order", foodId);

                    OrderItem orderItem = new OrderItem();
                    orderItem.setFoodItem(food);
                    orderItem.setQuantity(1);
                    orderItem.setOrder(order);

                    return orderItem;

                })
                .toList();

        order.setOrderItems(orderItems);

        BigDecimal total = orderItems.stream()
                .map(item -> item.getFoodItem()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);

        log.info("Total order amount calculated: {}", total);

        Order saved = orderRepository.save(order);

        log.info("Order saved successfully with ID: {}", saved.getId());

        OrderResponseDTO response = OrderMapper.toDTO(saved);

        //  EVENT TRIGGER
        log.info("Publishing order created event for orderId: {}", saved.getId());
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