package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderWebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderService orderService;


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

        // KITCHEN QUEUE - full queue with priority + ETA
        try {
            List<OrderResponseDTO> updatedQueue = orderService.buildKitchenQueueWithETA();

            messagingTemplate.convertAndSend(
                    "/topic/kitchen/orders",
                    updatedQueue
            );

            log.info("Kitchen queue broadcasted. size={}", updatedQueue.size());

        } catch (Exception ex) {
            log.error("Failed to broadcast kitchen queue update", ex);
        }


        //  ADMIN / MANAGER
        messagingTemplate.convertAndSend(
                "/topic/admin/orders",
                order
        );

        //  USER SPECIFIC
        Long userId = order.getUser().getId();

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                order
        );
    }
}