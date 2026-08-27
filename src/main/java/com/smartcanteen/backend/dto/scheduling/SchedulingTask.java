package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

public record SchedulingTask(
        Long orderId,
        Long orderItemId,
        Long foodItemId,
        Integer quantity,
        KitchenResourceType requiredResource,
        int durationMinutes
) {}
