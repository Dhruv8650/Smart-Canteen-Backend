package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;

import java.util.List;


public interface OrderService {
    OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail);

    OrderResponseDTO placePosOrder(OrderRequestDTO request, String adminEmail);

    List<OrderResponseDTO> getUserOrder(String userEmail);

    OrderResponseDTO getOrderById(Long orderId);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus);

    void reorder(Long orderId);

    List<OrderResponseDTO> getOrdersByStatuses(List<OrderStatus> statuses);

    OrderResponseDTO approvePayment(Long orderId);

    void cancelOrder(Long orderId);

    OrderResponseDTO rejectOrder(Long orderId);

    boolean hasActiveOrders();

    OrderResponseDTO verifyAndReturn(String pickupCode);
}
