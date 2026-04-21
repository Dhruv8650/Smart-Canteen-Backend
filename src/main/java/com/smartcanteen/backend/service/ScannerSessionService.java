package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.ScannerSession;
import com.smartcanteen.backend.repository.ScannerSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScannerSessionService {

    private final ScannerSessionRepository repository;

    @Transactional
    public ScannerSession createSession(String managerEmail) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        return repository.findFirstByManagerEmailAndActiveTrueAndExpiresAtAfter(managerEmail, now)
                .orElseGet(() -> repository.save(
                        ScannerSession.builder()
                                .token(UUID.randomUUID().toString())
                                .managerEmail(managerEmail)
                                .active(true)
                                .expiresAt(now.plusMinutes(30))
                                .build()
                ));
    }

    @Transactional
    public void revokeSession(String managerEmail) {

        repository.deactivateByManagerEmail(managerEmail);
    }

    public boolean isValid(String token) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        return repository.findByTokenAndActiveTrue(token)
                .filter(s -> s.getExpiresAt().isAfter(nowUtc))
                .isPresent();
    }

    public String getManagerEmail(String token) {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        return repository.findByTokenAndActiveTrue(token)
                .filter(s -> s.getExpiresAt().isAfter(nowUtc))
                .map(ScannerSession::getManagerEmail)
                .orElse(null);
    }
}
