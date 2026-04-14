package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // USER PLACES ORDER
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> placeOrder(
            @Valid @RequestBody OrderRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();

        OrderResponseDTO order = orderService.placeOrder(request, email);

        ApiResponse<OrderResponseDTO> response = ApiResponse.<OrderResponseDTO>builder()
                .success(true)
                .message("Order placed successfully")
                .data(order)
                .build();

        return ResponseEntity.ok(response);
    }

    // USER SEE OWN ORDERS
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getMyOrders(
            Authentication authentication) {

        String email = authentication.getName();

        List<OrderResponseDTO> orders = orderService.getUserOrder(email);

        ApiResponse<List<OrderResponseDTO>> response = ApiResponse.<List<OrderResponseDTO>>builder()
                .success(true)
                .message("User orders fetched successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrderById(
            @PathVariable Long orderId) {

        OrderResponseDTO order = orderService.getOrderById(orderId);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order fetched successfully")
                        .data(order)
                        .build()
        );
    }

    //  ADMIN SEE ALL ORDERS
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getAllOrders() {

        List<OrderResponseDTO> orders = orderService.getAllOrders();

        ApiResponse<List<OrderResponseDTO>> response = ApiResponse.<List<OrderResponseDTO>>builder()
                .success(true)
                .message("All orders fetched successfully")
                .data(orders)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/invoice")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId) {

        byte[] pdf = orderService.generateInvoice(orderId);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=invoice_" + orderId + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{orderId}/reorder")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> reorder(@PathVariable Long orderId) {

        orderService.reorder(orderId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Items added to cart")
                        .build()
        );
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId) {

        orderService.cancelOrder(orderId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Order cancelled successfully")
                        .build()
        );
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<String> verifyOrder(@RequestParam String pickupCode) {

        Order order = orderRepository.findByPickupCode(pickupCode)
                .orElseThrow(() -> new RuntimeException("Invalid QR"));

        if (order.getStatus() != OrderStatus.READY) {
            throw new RuntimeException("Order not ready or already collected");
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        return ResponseEntity.ok("Order verified & collected");
    }
}
