package com.smartcanteen.backend.dto.response.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KitchenScheduleBottleneckResponseDTO {

    private KitchenResourceType resource;
    private long workloadMinutes;
    private double congestion;
    private double pressure;
}
