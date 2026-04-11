package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.UpdateUserRoleDTO;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.service.CanteenService;
import com.smartcanteen.backend.service.OrderService;
import com.smartcanteen.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final CanteenService canteenService;

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

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getOrders(
            @RequestParam(required = false) List<OrderStatus> statuses
    ) {

        List<OrderResponseDTO> orders;

        if (statuses != null && !statuses.isEmpty()) {
            orders = orderService.getOrdersByStatuses(statuses);
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

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getUsers(
            @RequestParam(required = false) Role role) {

        List<UserResponseDTO> users;

        if (role != null) {
            users = userService.getUsersByRole(role);
        } else {
            users = userService.getAllUsers();
        }

        return ResponseEntity.ok(
                ApiResponse.<List<UserResponseDTO>>builder()
                        .success(true)
                        .message("Users fetched successfully")
                        .data(users)
                        .build()
        );
    }
    @PostMapping("/canteen/open")
    public ResponseEntity<String> openCanteen() {
        canteenService.openCanteen();
        return ResponseEntity.ok("Canteen opened");
    }

    @PostMapping("/canteen/close")
    public ResponseEntity<String> closeCanteen() {
        canteenService.closeCanteen();
        return ResponseEntity.ok("Canteen closed");
    }

    @PatchMapping("/orders/{orderId}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> rejectOrder(
            @PathVariable Long orderId) {

        OrderResponseDTO response = orderService.rejectOrder(orderId);

        return ResponseEntity.ok(
                ApiResponse.<OrderResponseDTO>builder()
                        .success(true)
                        .message("Order rejected successfully")
                        .data(response)
                        .build()
        );
    }
    @GetMapping("/canteen/status")
    public ResponseEntity<ApiResponse<Boolean>> getCanteenStatus() {

        boolean isOpen = canteenService.isCanteenOpen();

        return ResponseEntity.ok(
                ApiResponse.<Boolean>builder()
                        .success(true)
                        .message("Canteen status fetched")
                        .data(isOpen)
                        .build()
        );
    }
}