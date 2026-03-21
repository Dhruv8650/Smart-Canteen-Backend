package com.smartcanteen.backend.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MonthlyRevenueDTO {

    private LocalDateTime monthStart;
    private BigDecimal revenue;

    public MonthlyRevenueDTO(LocalDateTime monthStart, BigDecimal revenue) {
        this.monthStart = monthStart;
        this.revenue = revenue;
    }

    public LocalDateTime getMonthStart() {
        return monthStart;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}