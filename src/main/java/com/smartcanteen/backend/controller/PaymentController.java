package com.smartcanteen.backend.controller;


import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.OrderRequestDTO;
import com.smartcanteen.backend.dto.request.PaymentVerifyRequestDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentOrder(
            @Valid @RequestBody OrderRequestDTO request,
            Authentication authentication
    ) {
        Map<String, Object> data = paymentService.createPaymentOrder(request, authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Payment order created successfully")
                        .data(data)
                        .build()
        );
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequestDTO request,
            Authentication authentication
    ) {
        OrderResponseDTO order = paymentService.verifyPayment(request, authentication.getName());

        return ResponseEntity.ok(
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Payment verified and order created successfully")
                        .data(order)
                        .build()
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed");
    }

}
