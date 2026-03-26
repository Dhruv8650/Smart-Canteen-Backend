package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.request.RegisterRequestDTO;
import com.smartcanteen.backend.dto.response.AuthResponseDTO;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;

public interface UserService {
    User registerUser(RegisterRequestDTO request);

    AuthResponseDTO login(String email, String password);

    void updateUserRole(Long userId, Role role);
}
