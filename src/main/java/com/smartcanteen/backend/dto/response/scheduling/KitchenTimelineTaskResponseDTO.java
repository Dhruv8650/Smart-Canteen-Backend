package com.smartcanteen.backend.dto.response.scheduling;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class KitchenTimelineTaskResponseDTO {

    private Long orderId;
    private Long orderItemId;
    private Long foodItemId;
    private int durationMinutes;
    private int sequence;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
