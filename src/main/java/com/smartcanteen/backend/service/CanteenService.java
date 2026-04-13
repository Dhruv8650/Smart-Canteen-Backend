package com.smartcanteen.backend.service;

import com.smartcanteen.backend.entity.Canteen;
import com.smartcanteen.backend.entity.CanteenStatus;
import com.smartcanteen.backend.repository.CanteenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CanteenService {

    private final CanteenRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    //  Get or create canteen
    public Canteen getCanteen() {
        return repository.findById(1L)
                .orElseGet(() -> {
                    Canteen c = new Canteen();
                    c.setId(1L);
                    c.setStatus(CanteenStatus.CLOSED);
                    return repository.save(c);
                });
    }

    public void save(Canteen canteen) {
        repository.save(canteen);

        //  broadcast update
        messagingTemplate.convertAndSend(
                "/topic/canteen/status",
                canteen
        );
    }

    //  OPENING
    public void startOpening() {
        Canteen c = getCanteen();

        if (c.getStatus() == CanteenStatus.OPEN) return;

        c.setStatus(CanteenStatus.OPENING);
        c.setClosingSoonUntil(null); // reset

        // reset readiness
        c.setKitchenReady(false);
        c.setManagerReady(false);

        save(c);
    }

    //  OPEN
    public void setOpen() {
        Canteen c = getCanteen();
        c.setStatus(CanteenStatus.OPEN);
        save(c);
    }

    //  Closing warning
    public void startClosingSoon() {
        Canteen c = getCanteen();

        if (c.getStatus() != CanteenStatus.OPEN) {
            throw new IllegalStateException("Canteen must be OPEN to start closing");
        }

        c.setClosingSoonUntil(LocalDateTime.now().plusMinutes(5));
        save(c);
    }

    //  Move to closing
    public void startClosing() {
        Canteen c = getCanteen();
        c.setStatus(CanteenStatus.CLOSING);
        c.setClosingSoonUntil(null);
        save(c);
    }

    //  Final close
    public void setClosed() {
        Canteen c = getCanteen();
        c.setStatus(CanteenStatus.CLOSED);
        save(c);
    }

    //  Order check
    public boolean canAcceptOrders() {
        return getCanteen().getStatus() == CanteenStatus.OPEN;
    }

    public void setKitchenReady() {
        Canteen c = getCanteen();

        c.setKitchenReady(true);
        save(c);

        checkAndOpen(c);
    }

    public void setManagerReady() {
        Canteen c = getCanteen();

        c.setManagerReady(true);
        save(c);

        checkAndOpen(c);
    }

    private void checkAndOpen(Canteen c) {
        if (c.isKitchenReady() && c.isManagerReady()) {
            c.setStatus(CanteenStatus.OPEN);
            save(c);
        }
    }
}