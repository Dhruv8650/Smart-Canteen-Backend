package com.smartcanteen.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Canteen {

    @Id
    private Long id = 1L; // single canteen

    @Enumerated(EnumType.STRING)
    private CanteenStatus status;


    private LocalDateTime closingSoonUntil;

    private boolean kitchenReady;

    private boolean managerReady;
}
