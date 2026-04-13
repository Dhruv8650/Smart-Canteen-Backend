package com.smartcanteen.backend.config;

import com.smartcanteen.backend.entity.Canteen;
import com.smartcanteen.backend.service.CanteenService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CanteenScheduler {

    private final CanteenService canteenService;

    @Scheduled(fixedRate = 60000)
    public void handleClosingSoon() {

        Canteen c = canteenService.getCanteen();

        if (c.getClosingSoonUntil() != null &&
                LocalDateTime.now().isAfter(c.getClosingSoonUntil())) {

            canteenService.startClosing();
        }
    }
}
