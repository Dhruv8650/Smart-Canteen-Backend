package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class RoutingService {

    public void applyRouting(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (order.hasCookedItems()) {
            order.setStatus(OrderStatus.PENDING);
            return;
        }

        order.setStatus(OrderStatus.READY);

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        order.setReadyAt(nowUtc);
        order.setPickupExpiry(nowUtc.plusMinutes(45));
    }
}
