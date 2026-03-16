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

public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }


    @GetMapping("/revenue/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DailyRevenueDTO> getDailyRevenue() {
        return analyticsService.getDailyRevenue();
    }


    @GetMapping("/orders/status")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderStatusCountDTO> getOrderStatusCounts() {
        return analyticsService.getOrderStatusCounts();
    }


    @GetMapping("/top-items")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TopItemDTO> getTopSellingItems() {
        return analyticsService.getTopSellingItems();
    }


    @GetMapping("/category-sales")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CategorySalesDTO> getCategorySales() {
        return analyticsService.getCategorySales();
    }
}
