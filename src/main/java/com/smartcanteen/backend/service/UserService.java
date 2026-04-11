package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.OtpType;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;

import java.util.List;

public interface UserService {
    User registerUser(RegisterRequestDTO request);

    AuthResponseDTO login(String email, String password);

    void updateUserRole(Long userId, Role role);

    void logout(String token);

    void sendOtp(String email, OtpType type);

    void resetPasswordWithOtp(String email, String otp, String newPassword);

    void resendOtp(String email,OtpType type);

    List<UserResponseDTO> getUsersByRole(Role role);

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserByEmail(String email);

    void verifyEmail(String email, String otp);

    void forgotPassword(String email);
}
