package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

public record ResourceBottleneck(
        KitchenResourceType resource,
        long workloadMinutes,
        double congestion,
        double pressure
) {
}
