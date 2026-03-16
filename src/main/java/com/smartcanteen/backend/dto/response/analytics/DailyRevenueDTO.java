package com.smartcanteen.backend.dto.response.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyRevenueDTO {

    private LocalDate date;
    private BigDecimal revenue;

    public DailyRevenueDTO(LocalDateTime createdAt, BigDecimal revenue) {
        this.date = createdAt.toLocalDate();
        this.revenue = revenue;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}