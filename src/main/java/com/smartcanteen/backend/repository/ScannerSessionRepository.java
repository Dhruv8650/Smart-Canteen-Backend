package com.smartcanteen.backend.repository;

import com.smartcanteen.backend.entity.ScannerSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ScannerSessionRepository extends JpaRepository<ScannerSession, Long> {

    Optional<ScannerSession> findByTokenAndActiveTrue(String token);

    @Modifying
    @Query("""
    UPDATE ScannerSession s
    SET s.active = false
    WHERE s.managerEmail = :managerEmail
""")
    void deactivateByManagerEmail(String managerEmail);
}