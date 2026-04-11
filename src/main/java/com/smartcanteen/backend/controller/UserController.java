package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.ForgotPasswordRequestDTO;
import com.smartcanteen.backend.dto.request.LoginRequestDTO;
import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.request.ResetPasswordRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.OtpType;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.service.JwtService;
import com.smartcanteen.backend.service.TokenBlacklistService;
import com.smartcanteen.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public UserController(UserService userService, JwtService jwtService, TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.tokenBlacklistService = tokenBlacklistService;
    }
    @Autowired
    private RedisTemplate<String,String > redisTemplate;

    //  REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        User savedUser = userService.registerUser(request);

        UserResponseDTO userResponse = new UserResponseDTO(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isActive());

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



    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            return ResponseEntity.badRequest().body("Invalid token");
        }

        String token = authHeader.substring(7);

        userService.logout(token);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO request) {

        userService.forgotPassword(request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("OTP sent to email")
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody ResetPasswordRequestDTO request) {

        userService.resetPasswordWithOtp(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Password reset successful")
                        .build()
        );
    }


    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @RequestParam String email,
            @RequestParam String type) {

        userService.resendOtp(email, OtpType.valueOf(type));

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("OTP resent successfully")
                        .build()
        );
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestParam String email,
            @RequestParam String otp) {

        userService.verifyEmail(email, otp);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Email verified successfully")
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        String email = jwtService.extractEmail(refreshToken);

        // VALIDATE FROM REDIS
        if (!tokenBlacklistService.isValidRefreshToken(email, refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(email);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(Authentication authentication) {

        String email = authentication.getName();

        UserResponseDTO user = userService.getUserByEmail(email);

        return ResponseEntity.ok(
                ApiResponse.<UserResponseDTO>builder()
                        .success(true)
                        .message("User fetched successfully")
                        .data(user)
                        .build()
        );
    }
}