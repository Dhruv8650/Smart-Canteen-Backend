package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Order;

import java.util.List;


public interface OrderService {
    Order placeOrder(List<Long>foodIds,String userEmail);

    List<Order> getUserOrder(String userEmail);

    List<Order> getAllOrders();
}
