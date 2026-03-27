package com.smartcanteen.backend.service;

public interface TokenBlacklistService {

    void blacklistToken(String token);

    boolean isBlacklisted(String token);
}
