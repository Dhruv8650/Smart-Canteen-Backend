package com.smartcanteen.backend.dto.response;

public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private UserResponseDTO user;

    public AuthResponseDTO(String accessToken, String refreshToken, UserResponseDTO user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public UserResponseDTO getUser() {
        return user;
    }
}