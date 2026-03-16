package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO;
import com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO;
import com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO;
import com.smartcanteen.backend.dto.response.analytics.TopItemDTO;
import com.smartcanteen.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    AnalyticsService analyticsService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/daily")
    public List<DailyRevenueDTO> getDailyRevenue() {
        return analyticsService.getDailyRevenue();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/status")
    public List<OrderStatusCountDTO> getOrderStatusCounts() {
        return analyticsService.getOrderStatusCounts();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/top-items")
    public List<TopItemDTO> getTopSellingItems() {
        return analyticsService.getTopSellingItems();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/category-sales")
    public List<CategorySalesDTO> getCategorySales() {
        return analyticsService.getCategorySales();
    }
}
