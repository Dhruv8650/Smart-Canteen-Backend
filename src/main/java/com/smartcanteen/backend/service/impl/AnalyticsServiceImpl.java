package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.response.analytics.CategorySalesDTO;
import com.smartcanteen.backend.dto.response.analytics.DailyRevenueDTO;
import com.smartcanteen.backend.dto.response.analytics.OrderStatusCountDTO;
import com.smartcanteen.backend.dto.response.analytics.TopItemDTO;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;

    @Override
    public List<DailyRevenueDTO> getDailyRevenue() {
        return orderRepository.getDailyRevenue();
    }

    @Override
    public List<OrderStatusCountDTO> getOrderStatusCounts() {
        return orderRepository.getOrderStatusCounts();
    }

    @Override
    public List<TopItemDTO> getTopSellingItems() {
        return orderRepository.getTopSellingItems();
    }

    @Override
    public List<CategorySalesDTO> getCategorySales() {
        return orderRepository.getCategorySales();
    }
}
