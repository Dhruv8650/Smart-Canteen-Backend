package com.smartcanteen.backend.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WeeklyRevenueDTO {

    private LocalDateTime weekStart;
    private BigDecimal revenue;

    public WeeklyRevenueDTO(LocalDateTime weekStart, BigDecimal revenue) {
        this.weekStart = weekStart;
        this.revenue = revenue;
    }

    public LocalDateTime getWeekStart() {
        return weekStart;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}