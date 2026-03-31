package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.EmailAlreadyExistException;
import com.smartcanteen.backend.exception.InvalidCredentialsException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.UserMapper;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.EmailService;
import com.smartcanteen.backend.service.JwtService;
import com.smartcanteen.backend.service.TokenBlacklistService;
import com.smartcanteen.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;

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
                user.getRole(),
                user.isActive());

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
    public void sendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        user.setOtpAttempts(0); // reset attempts
        user.setLastOtpSentAt(LocalDateTime.now());

        userRepository.save(user);

        emailService.sendEmail(
                email,
                "Password Reset OTP",
                "Your OTP is: " + otp
        );
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        //  Check attempts
        if (user.getOtpAttempts() >= 3) {
            throw new RuntimeException("Maximum OTP attempts exceeded. Request new OTP.");
        }

        //  Check expiry
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        //  Validate OTP
        if (!otp.equals(user.getResetOtp())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1); // increment
            userRepository.save(user);
            throw new RuntimeException("Invalid OTP");
        }

        //  SUCCESS
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);

        userRepository.save(user);
    }

    @Override
    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        //  Prevent spam (30 sec cooldown)
        if (user.getLastOtpSentAt() != null &&
                user.getLastOtpSentAt().plusSeconds(30).isAfter(LocalDateTime.now())) {

            throw new RuntimeException("Please wait before requesting another OTP");
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        user.setResetOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setOtpAttempts(0);
        user.setLastOtpSentAt(LocalDateTime.now());

        userRepository.save(user);

        emailService.sendEmail(
                email,
                "Resend OTP",
                "Your new OTP is: " + otp
        );
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    public List<UserResponseDTO> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserMapper.toDTO(user);
    }

}