package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

public record ResourceWorkload(
        KitchenResourceType resource,
        long workloadMinutes,
        double congestion,
        double pressure
) {
}
