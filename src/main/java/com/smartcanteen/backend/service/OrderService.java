package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;


public interface OrderService {
    OrderResponseDTO placeOrder(OrderRequestDTO request, String userEmail);

    OrderResponseDTO placePosOrder(OrderRequestDTO request, String adminEmail);

    BigDecimal calculateOrderAmount(OrderRequestDTO request, String userEmail);

    OrderResponseDTO placeVerifiedOnlineOrder(OrderRequestDTO request,
                                              String userEmail,
                                              String paymentOrderId,
                                              String paymentId,
                                              String paymentSignature,
                                              BigDecimal paidAmount);

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
