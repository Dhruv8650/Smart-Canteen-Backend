package com.smartcanteen.backend.dto.response;

import lombok.RequiredArgsConstructor;


public class AuthResponseDTO {
    private String token;
    private UserResponseDTO user;

    public AuthResponseDTO(String token, UserResponseDTO user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public UserResponseDTO getUser() {
        return user;
    }
}
