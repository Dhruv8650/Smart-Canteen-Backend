package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleOrderEvent(OrderCreatedEvent event) {

        System.out.println(" Sending WebSocket update...");

        //  Extract order from events
        OrderResponseDTO order = event.getOrder();

        // Send to ADMIN dashboard
        messagingTemplate.convertAndSend(
                "/topic/admin/orders",
                order
        );

        // Send to specific USER
        Long userId = order.getUser().getId();

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                order
        );
    }
}