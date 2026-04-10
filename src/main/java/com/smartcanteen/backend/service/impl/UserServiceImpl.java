package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.OtpType;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.exception.EmailAlreadyExistException;
import com.smartcanteen.backend.exception.InvalidCredentialsException;
import com.smartcanteen.backend.exception.UserNotFoundException;
import com.smartcanteen.backend.mapper.UserMapper;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final CanteenService canteenService;

    @Override
    public User registerUser(RegisterRequestDTO request) {

        log.info("Registering user with email: {}", request.getEmail());

        //  Check existing user
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new EmailAlreadyExistException("Email already exists");
        }

        //  Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isVerified(false)
                .build();

        //  Save user
        User savedUser = userRepository.save(user);

        log.info("User saved successfully, sending OTP asynchronously...");

        //  Send OTP asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                sendOtp(savedUser.getEmail(), OtpType.VERIFY_EMAIL);
                log.info("OTP sent successfully to {}", savedUser.getEmail());
            } catch (Exception e) {
                log.error("Failed to send OTP to {}: {}", savedUser.getEmail(), e.getMessage());
            }
        });

        //  Return immediately (NO WAIT)
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

        if (!user.isVerified()) {
            throw new RuntimeException("Please verify your email first");
        }

        // CANTEEN CHECK
        if ((user.getRole() == Role.MANAGER || user.getRole() == Role.KITCHEN)
                && !canteenService.isCanteenOpen()) {

            log.warn("Login blocked - canteen closed for {}", email);
            throw new RuntimeException("Canteen is closed. Login not allowed.");
        }

        // Generate BOTH tokens
        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        // STORE REFRESH TOKEN IN REDIS
        tokenBlacklistService.storeRefreshToken(user.getEmail(), refreshToken);

        log.info("Tokens generated for user: {}", email);

        UserResponseDTO userDTO = new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive());

        log.info("Login successful for user: {}", email);

        return new AuthResponseDTO(accessToken, refreshToken, userDTO);
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
    public void logout(String token) {

        log.info("Logging out user, blacklisting token");

        String email = jwtService.extractEmail(token);

        // blacklist access token
        tokenBlacklistService.blacklistToken(token);

        // delete refresh token
        tokenBlacklistService.deleteRefreshToken(email);
    }

    @Override
    public void sendOtp(String email, OtpType type) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        user.setOtpAttempts(0);
        user.setLastOtpSentAt(LocalDateTime.now());

        if (type == OtpType.VERIFY_EMAIL) {
            user.setVerifyOtp(otp);
            user.setVerifyOtpExpiry(LocalDateTime.now().plusMinutes(5));
        } else {
            user.setResetOtp(otp);
            user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        }

        userRepository.save(user);

        String subject = (type == OtpType.VERIFY_EMAIL)
                ? "Email Verification OTP"
                : "Password Reset OTP";

        String message = (type == OtpType.VERIFY_EMAIL)
                ? "Use this OTP to verify your email: " + otp
                : "Use this OTP to reset your password: " + otp;

        emailService.sendEmail(email, subject, message);
    }

    @Override
    public void verifyEmail(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getOtpAttempts() >= 3) {
            throw new RuntimeException("Maximum OTP attempts exceeded");
        }

        if (user.getVerifyOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.equals(user.getVerifyOtp())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Invalid OTP");
        }

        // SUCCESS
        user.setVerified(true);

        user.setVerifyOtp(null);
        user.setVerifyOtpExpiry(null);
        user.setOtpAttempts(0);

        userRepository.save(user);
    }

    @Override
    public void resetPasswordWithOtp(String email, String otp, String newPassword) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getOtpAttempts() >= 3) {
            throw new RuntimeException("Maximum OTP attempts exceeded");
        }

        if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otp.equals(user.getResetOtp())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepository.save(user);
            throw new RuntimeException("Invalid OTP");
        }

        // SUCCESS
        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        user.setOtpAttempts(0);

        userRepository.save(user);
    }

    @Override
    public void resendOtp(String email, OtpType type) {

        System.out.println(" RESEND OTP CALLED: " + email + " TYPE: " + type);

        try {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println(" USER FOUND: " + user.getEmail());

            //  Prevent spam
            if (user.getLastOtpSentAt() != null &&
                    user.getLastOtpSentAt().plusSeconds(30).isAfter(LocalDateTime.now())) {

                throw new RuntimeException("Please wait before requesting another OTP");
            }

            String otp = String.valueOf(new Random().nextInt(900000) + 100000);

            user.setOtpAttempts(0);
            user.setLastOtpSentAt(LocalDateTime.now());

            String subject;
            String message;

            if (type == OtpType.VERIFY_EMAIL) {

                user.setVerifyOtp(otp);
                user.setVerifyOtpExpiry(LocalDateTime.now().plusMinutes(5));

                subject = "Email Verification OTP";
                message = "Your verification OTP is: " + otp;

            } else {

                user.setResetOtp(otp);
                user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));

                subject = "Password Reset OTP";
                message = "Your password reset OTP is: " + otp;
            }

            userRepository.save(user);

            System.out.println(" SENDING EMAIL...");

            emailService.sendEmail(email, subject, message);

            System.out.println(" OTP SENT SUCCESSFULLY");

        } catch (Exception e) {
            System.out.println(" ERROR IN RESEND OTP:");
            e.printStackTrace();
            throw e;
        }
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