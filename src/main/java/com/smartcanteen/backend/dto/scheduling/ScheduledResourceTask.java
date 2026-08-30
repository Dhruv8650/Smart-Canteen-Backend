package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

public record ScheduledResourceTask(
        Long orderId,
        Long orderItemId,
        Long foodItemId,
        KitchenResourceType resource,
        int durationMinutes,
        int sequence
) {
}
