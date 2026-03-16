package com.smartcanteen.backend.dto.response.analytics;

import com.smartcanteen.backend.entity.OrderStatus;

public record OrderStatusCountDTO(
        OrderStatus status,
        Long count
) {
}
