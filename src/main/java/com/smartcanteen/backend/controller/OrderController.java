package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.response.InvoiceResponseDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.service.InvoiceService;
import com.smartcanteen.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;

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

    @GetMapping(value = "/{orderId}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId
    ) {

        byte[] pdf = invoiceService.generateInvoice(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice_" + orderId + ".pdf")
                .body(pdf);
    }

    @GetMapping("/{orderId}/invoice-preview")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getInvoicePreview(
            @PathVariable Long orderId
    ) {

        InvoiceResponseDTO invoice = invoiceService.getInvoiceData(orderId);

        return ResponseEntity.ok(
                ApiResponse.<InvoiceResponseDTO>builder()
                        .success(true)
                        .message("Invoice fetched successfully")
                        .data(invoice)
                        .build()
        );
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

    @GetMapping("/verify")
    public ResponseEntity<String> verifyOrder(@RequestParam("code") String pickupCode) {

        OrderResponseDTO order = orderService.verifyAndReturn(pickupCode);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body("""
            <html>
                <body style="text-align:center;
                             font-family:sans-serif;
                             margin-top:50px;">
                    <h1 style="color:green;"> Order Verified</h1>
                    <h2>Order #""" + order.getId() + """
                    </h2>
                    <p style="font-size:18px;">Successfully collected</p>
                </body>
            </html>
        """);
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOrderPost(
            @RequestBody Map<String, String> body
    ) {

        String pickupCode = body.get("code");

        OrderResponseDTO order = orderService.verifyAndReturn(pickupCode);

        Map<String, Object> data = Map.of(
                "orderId", order.getId(),
                "message", "Order verified successfully",
                "status", "COMPLETED"
        );

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Order #" + order.getId() + " verified and completed")
                        .data(data)
                        .build()
        );
    }
}
