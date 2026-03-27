package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.request.LoginRequestDTO;
import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.BlackListedToken;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.BlackListedTokenRepository;
import com.smartcanteen.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final BlackListedTokenRepository blackListedTokenRepository;

    public UserController(UserService userService,BlackListedTokenRepository blackListedTokenRepository){
        this.userService = userService;
        this.blackListedTokenRepository=blackListedTokenRepository;
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

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            return ResponseEntity.badRequest().body("Invalid token");
        }

        String token=authHeader.substring(7);

        BlackListedToken blackListedToken=new BlackListedToken();
        blackListedToken.setToken(token);
        blackListedToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        blackListedTokenRepository.save(blackListedToken);

        return ResponseEntity.ok("Logged out successfully");
    }
}