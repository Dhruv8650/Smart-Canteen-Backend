package com.smartcanteen.backend.dto.response.scheduling;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KitchenDispatchTaskResponseDTO {

    private Long orderId;
    private Long orderItemId;
    private Long foodItemId;
    private int durationMinutes;
    private int sequence;
}
