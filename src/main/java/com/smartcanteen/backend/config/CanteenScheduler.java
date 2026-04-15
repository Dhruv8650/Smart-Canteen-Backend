package com.smartcanteen.backend.config;

import com.smartcanteen.backend.entity.Canteen;
import com.smartcanteen.backend.entity.CanteenStatus;
import com.smartcanteen.backend.service.CanteenService;
import com.smartcanteen.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CanteenScheduler {

    private final CanteenService canteenService;
    private final OrderService orderService;

    @Scheduled(fixedRate = 10000) // every 1 min
    public void handleCanteenFlow() {

        Canteen c = canteenService.getCanteen();

        //  closingSoon → CLOSING
        if (c.getClosingSoonUntil() != null &&
                LocalDateTime.now().isAfter(c.getClosingSoonUntil())) {

            canteenService.startClosing();
            return;
        }

        // CLOSING → CLOSED (when no active orders)
        if (c.getStatus() == CanteenStatus.CLOSING) {

            boolean hasActiveOrders = orderService.hasActiveOrders();

            if (!hasActiveOrders) {
                canteenService.setClosed();
            }
        }
    }
}
