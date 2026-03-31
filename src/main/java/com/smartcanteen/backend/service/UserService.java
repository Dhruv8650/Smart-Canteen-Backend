package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;

import java.util.List;

public interface UserService {
    User registerUser(RegisterRequestDTO request);

    AuthResponseDTO login(String email, String password);

    void updateUserRole(Long userId, Role role);

    void logout(String token);

    void sendOtp(String email);

    void resetPasswordWithOtp(String email, String otp, String newPassword);

    void resendOtp(String email);

    List<UserResponseDTO> getUsersByRole(Role role);

    List<UserResponseDTO> getAllUsers();
}
