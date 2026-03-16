package com.smartcanteen.backend.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DailyRevenueDTO(
        LocalDateTime date,
        BigDecimal revenue
) {}
