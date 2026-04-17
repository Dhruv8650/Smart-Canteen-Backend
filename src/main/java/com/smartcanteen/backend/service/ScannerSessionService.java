package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.ScannerSession;
import com.smartcanteen.backend.repository.ScannerSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScannerSessionService {

    private final ScannerSessionRepository repository;

    public ScannerSession createSession(String managerEmail) {

        // remove old session (optional: only one active)
        repository.deleteByManagerEmail(managerEmail);

        String token = UUID.randomUUID().toString();

        ScannerSession session = ScannerSession.builder()
                .token(token)
                .managerEmail(managerEmail)
                .active(true)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        return repository.save(session);
    }

    public void revokeSession(String managerEmail) {
        repository.deleteByManagerEmail(managerEmail);
    }

    public boolean isValid(String token) {

        return repository.findByTokenAndActiveTrue(token)
                .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    public String getManagerEmail(String token) {
        return repository.findByTokenAndActiveTrue(token)
                .map(ScannerSession::getManagerEmail)
                .orElse(null);
    }
}