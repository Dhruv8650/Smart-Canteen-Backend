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
@RequestMapping("/manager")
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

    @GetMapping("/orders")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) List<OrderStatus> statuses) {

        List<OrderResponseDTO> orders;

        if (statuses != null && !statuses.isEmpty()) {
            orders = orderService.getOrdersByStatuses(statuses);
        } else if (status != null) {
            orders = orderService.getOrdersByStatuses(List.of(status));
        } else {
            orders = orderService.getAllOrders();
        }

        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Orders fetched successfully")
                        .data(orders)
                        .build()
        );
    }
}