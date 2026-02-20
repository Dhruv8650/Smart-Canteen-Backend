package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.service.OrderService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;

    public OrderServiceImpl(OrderRepository orderRepository,UserRepository userRepository,FoodItemRepository foodItemRepository){
        this.orderRepository=orderRepository;
        this.userRepository=userRepository;
        this.foodItemRepository=foodItemRepository;
    }

    @Override
    public Order placeOrder(List<Long> foodIds, String userEmail) {
        User user=userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new UserNotFoundException("User not found"));

        List<FoodItem> foodItems=foodItemRepository.findAllById(foodIds);

        double total=foodItems.stream()
                .mapToDouble(FoodItem::getPrice)
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setFoodItems(foodItems);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Override
    public List<Order> getUserOrder(String userEmail) {
        User user=userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
        return orderRepository.findByUser(user);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
