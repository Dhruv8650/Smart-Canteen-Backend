package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.response.analytics.*;

import java.util.List;

public interface AnalyticsService {
    List<DailyRevenueDTO> getDailyRevenue();

    List<WeeklyRevenueDTO> getWeeklyRevenue();

    List<MonthlyRevenueDTO> getMonthlyRevenue();

    List<OrderStatusCountDTO> getOrderStatusCounts();

    List<TopItemDTO> getTopSellingItems();

    List<CategorySalesDTO> getCategorySales();


}
