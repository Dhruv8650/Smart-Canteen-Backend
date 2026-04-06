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

@PreAuthorize("hasRole('KITCHEN')")
@RestController
@RequestMapping("/kitchen/orders")
@RequiredArgsConstructor
public class KitchenController {

    private final OrderService orderService;

    //  GET ACTIVE ORDERS
    @GetMapping

    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getKitchenOrders() {

        List<OrderResponseDTO> orders =
                orderService.getOrdersByStatuses(List.of(
                        OrderStatus.PENDING,
                        OrderStatus.PREPARING
                ));

        return ResponseEntity.ok(
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Kitchen orders fetched")
                        .data(orders)
                        .build()
        );
    }

    //  UPDATE STATUS (BUTTON ACTION)
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        OrderResponseDTO order =
                orderService.updateOrderStatus(orderId, status);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order status updated")
                        .data(order)
                        .build()
        );
    }
}