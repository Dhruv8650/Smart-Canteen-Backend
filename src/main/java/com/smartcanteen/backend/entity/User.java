package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true,nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String resetToken;

    private String resetOtp;

    private LocalDateTime resetTokenExpiry;

    private LocalDateTime otpExpiry;

    private int otpAttempts;

    private LocalDateTime lastOtpSentAt;


}
