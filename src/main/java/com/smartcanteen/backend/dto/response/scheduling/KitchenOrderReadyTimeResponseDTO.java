package com.smartcanteen.backend.dto.response.scheduling;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class KitchenOrderReadyTimeResponseDTO {

    private Long orderId;
    private LocalDateTime readyAt;
}
