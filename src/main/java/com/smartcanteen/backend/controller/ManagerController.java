package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final OrderService orderService;

    // ONLY COMPLETE ORDER
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> completeOrder(
            @PathVariable Long orderId) {

        OrderResponseDTO order =
                orderService.updateOrderStatus(orderId, OrderStatus.COMPLETED);

        ApiResponse<OrderResponseDTO> response =
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order marked as COMPLETED")
                        .data(order)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/monitor")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getActiveOrders() {

        List<OrderResponseDTO> orders = orderService.getActiveOrders();

        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Active orders fetched for monitoring")
                        .data(orders)
                        .build()
        );
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getReadyOrders() {

        List<OrderResponseDTO> orders = orderService.getOrdersByStatus(OrderStatus.READY);

        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Ready orders fetched")
                        .data(orders)
                        .build()
        );
    }
}