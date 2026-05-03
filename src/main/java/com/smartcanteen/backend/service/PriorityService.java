package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Order;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class PriorityService {

    public double calculatePriority(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        LocalDateTime createdAt = order.getCreatedAt();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        long waitingMinutes = createdAt == null
                ? 0
                : Math.max(0, Duration.between(createdAt, nowUtc).toMinutes());

        int totalPrepTime = order.getTotalPrepTime() == null || order.getTotalPrepTime() <= 0
                ? 1
                : order.getTotalPrepTime();

        double prepFactor = 1.0 / totalPrepTime;

        double waitingWeight = 0.5;

        return prepFactor + (waitingMinutes * waitingWeight);
    }
}
