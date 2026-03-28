package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.UpdateUserRoleDTO;
import com.smartcanteen.backend.service.OrderService;
import com.smartcanteen.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;

    // DASHBOARD
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<String>> adminDashboard() {

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Admin dashboard loaded")
                        .data("Welcome Admin!")
                        .build()
        );
    }

    //  PROMOTE USER (CORE API)
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateUserRoleDTO request) {

        userService.updateUserRole(userId, request.getRole());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("User role updated successfully")
                        .build()
        );
    }

    @PatchMapping("/orders/{orderId}/approve-payment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> approvePayment(@PathVariable Long orderId) {

        orderService.approvePayment(orderId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Payment approved, order moved to PENDING")
                        .build()
        );
    }
}