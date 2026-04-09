package com.smartcanteen.backend.service;

public interface TokenBlacklistService {

    void blacklistToken(String token);

    boolean isBlacklisted(String token);

    void storeRefreshToken(String email, String refreshToken);

    boolean isValidRefreshToken(String email, String token);

    void deleteRefreshToken(String email);

}
