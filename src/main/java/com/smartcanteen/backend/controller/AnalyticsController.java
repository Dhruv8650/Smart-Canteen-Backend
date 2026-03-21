package com.smartcanteen.backend.controller;

import com.smartcanteen.backend.dto.common.ApiResponse;
import com.smartcanteen.backend.dto.response.analytics.*;
import com.smartcanteen.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // applies to all methods
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    //  DAILY REVENUE
    @GetMapping("/revenue/daily")
    public ResponseEntity<ApiResponse<List<DailyRevenueDTO>>> getDailyRevenue() {

        List<DailyRevenueDTO> data = analyticsService.getDailyRevenue();

        return ResponseEntity.ok(ApiResponse.<List<DailyRevenueDTO>>builder()
                .success(true)
                .message("Daily revenue fetched successfully")
                .data(data)
                .build());
    }

    //  WEEKLY REVENUE
    @GetMapping("/revenue/weekly")
    public ResponseEntity<ApiResponse<List<WeeklyRevenueDTO>>> getWeeklyRevenue() {

        List<WeeklyRevenueDTO> data = analyticsService.getWeeklyRevenue();

        return ResponseEntity.ok(ApiResponse.<List<WeeklyRevenueDTO>>builder()
                .success(true)
                .message("Weekly revenue fetched successfully")
                .data(data)
                .build());
    }

    //  MONTHLY REVENUE
    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse<List<MonthlyRevenueDTO>>> getMonthlyRevenue() {

        List<MonthlyRevenueDTO> data = analyticsService.getMonthlyRevenue();

        return ResponseEntity.ok(ApiResponse.<List<MonthlyRevenueDTO>>builder()
                .success(true)
                .message("Monthly revenue fetched successfully")
                .data(data)
                .build());
    }

    //  ORDER STATUS COUNT
    @GetMapping("/orders/status")
    public ResponseEntity<ApiResponse<List<OrderStatusCountDTO>>> getOrderStatusCounts() {

        List<OrderStatusCountDTO> data = analyticsService.getOrderStatusCounts();

        return ResponseEntity.ok(ApiResponse.<List<OrderStatusCountDTO>>builder()
                .success(true)
                .message("Order status count fetched successfully")
                .data(data)
                .build());
    }

    //  TOP ITEMS
    @GetMapping("/top-items")
    public ResponseEntity<ApiResponse<List<TopItemDTO>>> getTopSellingItems() {

        List<TopItemDTO> data = analyticsService.getTopSellingItems();

        return ResponseEntity.ok(ApiResponse.<List<TopItemDTO>>builder()
                .success(true)
                .message("Top selling items fetched successfully")
                .data(data)
                .build());
    }

    //  CATEGORY SALES
    @GetMapping("/category-sales")
    public ResponseEntity<ApiResponse<List<CategorySalesDTO>>> getCategorySales() {

        List<CategorySalesDTO> data = analyticsService.getCategorySales();

        return ResponseEntity.ok(ApiResponse.<List<CategorySalesDTO>>builder()
                .success(true)
                .message("Category sales fetched successfully")
                .data(data)
                .build());
    }
}