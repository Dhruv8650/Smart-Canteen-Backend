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

    //  UPDATE ORDER STATUS
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatus status) {

        OrderResponseDTO order = orderService.updateOrderStatus(orderId, status);

        ApiResponse<OrderResponseDTO> response =
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order status updated successfully")
                        .data(order)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  GET PENDING ORDERS
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getPendingOrders() {

        List<OrderResponseDTO> orders = orderService.getPendingOrders();

        ApiResponse<List<OrderResponseDTO>> response =
                ApiResponse.<List<OrderResponseDTO>>builder()
                        .success(true)
                        .message("Pending orders fetched successfully")
                        .data(orders)
                        .build();

        return ResponseEntity.ok(response);
    }
}