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

    //  ORDER CREATED EVENT
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        sendOrderUpdate(event.getOrder());
    }

    //  NEW — ORDER STATUS UPDATED EVENT
    @EventListener
    public void handleOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
        sendOrderUpdate(event.getOrder());
    }

    //  COMMON METHOD (BEST PRACTICE)
    private void sendOrderUpdate(OrderResponseDTO order) {

        System.out.println(" Sending WebSocket update...");

        //  ADMIN / MANAGER dashboard
        messagingTemplate.convertAndSend(
                "/topic/admin/orders",
                order
        );

        //  USER specific updates
        Long userId = order.getUser().getId();

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                order
        );
    }
}