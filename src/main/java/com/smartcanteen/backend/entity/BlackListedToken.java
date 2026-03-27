package com.smartcanteen.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class BlackListedToken {

    @Id
    private String token;

    private LocalDateTime expiryDate;
}
