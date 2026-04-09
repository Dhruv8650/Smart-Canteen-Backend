package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.CanteenStatus;
import com.smartcanteen.backend.events.CanteenStatusEvent;
import com.smartcanteen.backend.repository.CanteenStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CanteenService {

    private final CanteenStatusRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public boolean isCanteenOpen() {
        return repository.findById(1L)
                .map(CanteenStatus::isOpen)
                .orElse(false);
    }

    public void openCanteen() {
        repository.save(new CanteenStatus(1L, true));

        //  SEND REAL-TIME EVENT
        messagingTemplate.convertAndSend(
                "/topic/canteen/status",
                new CanteenStatusEvent(true, "Canteen is now OPEN")
        );
    }

    public void closeCanteen() {
        repository.save(new CanteenStatus(1L, false));

        //  SEND REAL-TIME EVENT
        messagingTemplate.convertAndSend(
                "/topic/canteen/status",
                new CanteenStatusEvent(false, "Canteen is now CLOSED")
        );
    }
}