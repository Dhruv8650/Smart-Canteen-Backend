package com.smartcanteen.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class CanteenResponseDTO {
    private String status;
    private LocalDateTime closingSoonUntil;
    private boolean kitchenReady;
    private boolean managerReady;
}
