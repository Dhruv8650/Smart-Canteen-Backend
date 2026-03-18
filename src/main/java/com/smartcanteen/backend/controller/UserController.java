package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.LoginRequestDTO;
import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    //  REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        User savedUser = userService.registerUser(request);

        UserResponseDTO userResponse = new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        ApiResponse<UserResponseDTO> response =
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(userResponse)
                        .build();

        return ResponseEntity.ok(response);
    }

    //  LOGIN
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        AuthResponseDTO authResponse =
                userService.login(request.getEmail(), request.getPassword());

        ApiResponse<AuthResponseDTO> response =
                ApiResponse.<AuthResponseDTO>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authResponse)
                        .build();

        return ResponseEntity.ok(response);
    }
}