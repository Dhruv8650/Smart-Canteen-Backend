package com.smartcanteen.backend.dto.response.scheduling;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class KitchenScheduleSnapshotResponseDTO {

    private LocalDateTime scheduledAt;
    private int totalOrders;
    private int totalTasks;
    private KitchenScheduleBottleneckResponseDTO bottleneck;
    private List<KitchenResourceScheduleResponseDTO> resources;
    private List<KitchenOrderReadyTimeResponseDTO> orderReadyTimes;
}
