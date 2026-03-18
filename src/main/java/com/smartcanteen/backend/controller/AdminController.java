package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.CreateManagerRequestDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.User;
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

    //  DASHBOARD
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<String>> adminDashboard() {

        ApiResponse<String> response =
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Admin dashboard loaded")
                        .data("Welcome Admin!")
                        .build();

        return ResponseEntity.ok(response);
    }

    //  CREATE MANAGER
    @PostMapping("/create-manager")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createManager(
            @RequestBody CreateManagerRequestDTO request) {

        User manager = userService.createManager(
                request.getName(),
                request.getEmail(),
                request.getPassword()
        );

        UserResponseDTO dto = new UserResponseDTO(
                manager.getId(),
                manager.getName(),
                manager.getEmail(),
                manager.getRole()
        );

        ApiResponse<UserResponseDTO> response =
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("Manager created successfully")
                        .data(dto)
                        .build();

        return ResponseEntity.ok(response);
    }
}