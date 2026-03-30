package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.EmailAlreadyExistException;
import com.smartcanteen.backend.exception.InvalidCredentialsException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.JwtService;
import com.smartcanteen.backend.service.TokenBlacklistService;
import com.smartcanteen.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public User registerUser(RegisterRequestDTO request) {

        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new EmailAlreadyExistException("Email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        log.info("User registered successfully with ID: {}", savedUser.getId());

        return savedUser;
    }

    @Override
    public AuthResponseDTO login(String email, String password) {

        log.info("Login attempt for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Login failed - user not found: {}", email);
                    return new UserNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed - invalid credentials for email: {}", email);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        log.info("JWT token generated for user: {}", email);

        UserResponseDTO userDTO = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        log.info("Login successful for user: {}", email);

        return new AuthResponseDTO(token, userDTO);
    }

    public void updateUserRole(Long userId, Role role) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        //  GET CURRENT ADMIN EMAIL
        String currentEmail = SecurityUtils.getCurrentUserEmail();

        //  PREVENT SELF ROLE CHANGE
        if (user.getEmail().equals(currentEmail)) {
            throw new IllegalStateException("Admin cannot change own role");
        }

        user.setRole(role);

        userRepository.save(user);
    }

    @Override
    public void logout(String token){
        log.info("Logging out user, blacklisting token");
        tokenBlacklistService.blacklistToken(token);
    }

    @Override
    public void generateResetToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        // TODO: send email (for now log it)
        log.info("Reset link: http://localhost:5173/reset-password?token={}", token);
    }

    @Override
    public void resetPassword(String token, String newPassword) {

        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }
}