package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.entity.ScannerSession;
import com.smartcanteen.backend.security.SecurityUtils;
import com.smartcanteen.backend.service.ScannerSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/manager/scanner-session")
@RequiredArgsConstructor
public class ScannerSessionController {

    private final ScannerSessionService service;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> create() {

        String email = SecurityUtils.getCurrentUserEmail();

        ScannerSession session = service.createSession(email);

        String scannerUrl = frontendUrl + "/manager/external-scanner?token=" + session.getToken();

        return ResponseEntity.ok(Map.of(
                "scannerUrl", scannerUrl,
                "token", session.getToken()
        ));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> revoke() {

        String email = SecurityUtils.getCurrentUserEmail();

        service.revokeSession(email);

        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");

        boolean valid = service.isValid(token);

        return ResponseEntity.ok(Map.of("valid", valid));
    }
}