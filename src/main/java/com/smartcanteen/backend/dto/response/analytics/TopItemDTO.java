package com.smartcanteen.backend.dto.response.analytics;

public record TopItemDTO(
        Long foodItemId,
        String name,
        Long totalSold
) {}
