package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

import java.time.LocalDateTime;

public record ScheduledTaskTimeline(
        Long orderId,
        Long orderItemId,
        Long foodItemId,
        KitchenResourceType resource,
        int durationMinutes,
        int sequence,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
