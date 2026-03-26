package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.UpdateOrderStatusDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kitchen")
@RequiredArgsConstructor
@PreAuthorize("hasRole('KITCHEN')")
public class KitchenController {

    private final OrderService orderService;

    //  GET ACTIVE ORDERS (Kitchen Dashboard)
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getActiveOrders() {

        List<OrderResponseDTO> orders = orderService.getActiveOrders();

        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Active kitchen orders fetched")
                        .data(orders)
                        .build()
        );
    }

    // 3 UPDATE STATUS (Kitchen Flow)
    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusDTO request) {

        OrderStatus status = request.getStatus();

        // Validation (optional, can move to service)
        if (status != OrderStatus.PREPARING && status != OrderStatus.READY) {
            throw new IllegalArgumentException("Kitchen can only set PREPARING or READY");
        }

        OrderResponseDTO updated =
                orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order status updated by kitchen")
                        .data(updated)
                        .build()
        );
    }
}