package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO;
import com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO;
import com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO;
import com.smartcanteen.backend.dto.response.analytics.TopItemDTO;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;

    @Override
    public List<DailyRevenueDTO> getDailyRevenue() {

        log.info("Fetching daily revenue analytics");

        List<DailyRevenueDTO> data = orderRepository.getDailyRevenue();

        log.info("Daily revenue records fetched: {}", data.size());

        return data;
    }

    @Override
    public List<OrderStatusCountDTO> getOrderStatusCounts() {

        log.info("Fetching order status counts");

        List<OrderStatusCountDTO> data = orderRepository.getOrderStatusCounts();

        log.info("Order status count records fetched: {}", data.size());

        return data;
    }

    @Override
    public List<TopItemDTO> getTopSellingItems() {

        log.info("Fetching top selling items");

        List<TopItemDTO> data = orderRepository.getTopSellingItems();

        log.info("Top selling items fetched: {}", data.size());

        return data;
    }

    @Override
    public List<CategorySalesDTO> getCategorySales() {

        log.info("Fetching category sales analytics");

        List<CategorySalesDTO> data = orderRepository.getCategorySales();

        log.info("Category sales records fetched: {}", data.size());

        return data;
    }
}