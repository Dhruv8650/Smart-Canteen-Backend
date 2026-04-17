package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.ScannerSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScannerSessionRepository extends JpaRepository<ScannerSession, Long> {

    Optional<ScannerSession> findByTokenAndActiveTrue(String token);

    void deleteByManagerEmail(String managerEmail);
}