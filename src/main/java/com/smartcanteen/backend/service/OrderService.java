package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;

import java.util.List;


public interface OrderService {
    OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail);

    List<OrderResponseDTO> getUserOrder(String userEmail);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus);

    List<OrderResponseDTO> getPendingOrders();
}
