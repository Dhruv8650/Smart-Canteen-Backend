package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO;
import com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO;
import com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO;
import com.smartcanteen.backend.dto.response.analytics.TopItemDTO;

import java.util.List;

public interface AnalyticsService {
    List<DailyRevenueDTO> getDailyRevenue();

    List<OrderStatusCountDTO> getOrderStatusCounts();

    List<TopItemDTO> getTopSellingItems();

    List<CategorySalesDTO> getCategorySales();
}
