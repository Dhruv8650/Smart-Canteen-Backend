package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.OrderStatus;
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

        System.out.println("Sending WebSocket update...");

        // 🔥 KITCHEN ONLY (PENDING + PREPARING)
        if (order.getStatus() == OrderStatus.PENDING ||
                order.getStatus() == OrderStatus.PREPARING) {

            messagingTemplate.convertAndSend(
                    "/topic/kitchen/orders",
                    order
            );
        }

        // 🔥 ADMIN / MANAGER
        messagingTemplate.convertAndSend(
                "/topic/admin/orders",
                order
        );

        // 🔥 USER SPECIFIC
        Long userId = order.getUser().getId();

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                order
        );
    }
}