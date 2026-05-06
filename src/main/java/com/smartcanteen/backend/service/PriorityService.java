package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class PriorityService {

    public double calculatePriority(Order order, int queueLoad) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        LocalDateTime createdAt = order.getCreatedAt();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        long waitingMinutes = createdAt == null
                ? 0
                : Math.max(0, Duration.between(createdAt, nowUtc).toMinutes());

        // Prevent explosion
        long cappedWaiting = Math.min(waitingMinutes, 60);

        int totalPrepTime = order.getTotalPrepTime() == null || order.getTotalPrepTime() <= 0
                ? 1
                : order.getTotalPrepTime();

        // Adaptive weights
        double waitingWeight = queueLoad >= 10 ? 0.3 : 0.7;
        double prepWeight = queueLoad >= 10 ? 2.0 : 1.0;

        double prepFactor = prepWeight / totalPrepTime;

        // Status boost
        double statusBoost = 0;
        if (order.getStatus() == OrderStatus.PREPARING) {
            statusBoost = 0.5;
        }

        return prepFactor + (cappedWaiting * waitingWeight) + statusBoost;
    }
}
