package com.smartcanteen.backend.dto.response.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class KitchenResourceScheduleResponseDTO {

    private KitchenResourceType resource;
    private long workloadMinutes;
    private double congestion;
    private double pressure;
    private LocalDateTime availableAt;
    private int totalDurationMinutes;
    private List<KitchenDispatchTaskResponseDTO> dispatchQueue;
    private List<KitchenTimelineTaskResponseDTO> timeline;
}
