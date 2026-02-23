package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.OrderNotFoundException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.OrderMapper;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            UserRepository userRepository,
                            FoodItemRepository foodItemRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    public OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<FoodItem> foodItems =
                foodItemRepository.findAllById(request.getFoodItemIds());

        if (foodItems.isEmpty()) {
            throw new IllegalArgumentException("No valid food items selected");
        }

        BigDecimal total = foodItems.stream()
                .map(FoodItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setFoodItems(foodItems);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

        return OrderMapper.toDTO(saved);
    }

    @Override
    public List<OrderResponseDTO> getUserOrder(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return orderRepository.findByUser(user)
                .stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Override
    public List<OrderResponseDTO> getPendingOrders() {
        return orderRepository.findByStatus(OrderStatus.PENDING)
                .stream()
                .map(OrderMapper::toDTO)
                .toList();
    }

    @Transactional
    @Override
    public OrderResponseDTO updateOrderStatus(Long orderId,
                                              OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Order not found with id: " + orderId));

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        return OrderMapper.toDTO(order);
    }

    private void validateStatusTransition(OrderStatus current,
                                          OrderStatus next) {

        switch (current) {

            case PENDING -> {
                if (next != OrderStatus.PREPARING &&
                        next != OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                            "Invalid transition from PENDING");
                }
            }

            case PREPARING -> {
                if (next != OrderStatus.READY &&
                        next != OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                            "Invalid transition from PREPARING");
                }
            }

            case READY -> {
                if (next != OrderStatus.COMPLETED) {
                    throw new IllegalStateException(
                            "Invalid transition from READY");
                }
            }

            default -> throw new IllegalStateException(
                    "Order cannot be modified in current state");
        }
    }
}