package com.smartcanteen.backend.mapper;

import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.entity.User;

public class UserMapper {

    public static UserResponseDTO toDTO(User user) {

        if (user == null) return null;

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}
