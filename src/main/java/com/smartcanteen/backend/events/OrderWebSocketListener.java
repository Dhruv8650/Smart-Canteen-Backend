package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.scheduling.KitchenScheduleSnapshotResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
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

    private static final String KITCHEN_ORDERS_TOPIC =
            "/topic/kitchen/orders";

    private static final String KITCHEN_SCHEDULE_TOPIC =
            "/topic/kitchen/schedule";

    private static final String ADMIN_ORDERS_TOPIC =
            "/topic/admin/orders";

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderService orderService;

    // ORDER CREATED EVENT
    @EventListener
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        sendOrderUpdate(event.getOrder());
    }

    // ORDER STATUS UPDATED EVENT
    @EventListener
    public void handleOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
        sendOrderUpdate(event.getOrder());
    }

    // COMMON METHOD
    private void sendOrderUpdate(OrderResponseDTO order) {

        log.debug("Sending WebSocket order update");

        // KITCHEN QUEUE - full queue with priority + ETA
        try {
            List<OrderResponseDTO> updatedQueue =
                    orderService.buildKitchenQueueWithETA();

            messagingTemplate.convertAndSend(
                    KITCHEN_ORDERS_TOPIC,
                    updatedQueue
            );

            log.info(
                    "Kitchen queue broadcasted. size={}",
                    updatedQueue.size()
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to broadcast kitchen queue update",
                    ex
            );
        }

        // KITCHEN OPERATIONAL SCHEDULE
        sendKitchenScheduleUpdate();

        // ADMIN / MANAGER
        messagingTemplate.convertAndSend(
                ADMIN_ORDERS_TOPIC,
                order
        );

        // USER SPECIFIC
        Long userId = order.getUser().getId();

        messagingTemplate.convertAndSend(
                "/topic/user/" + userId,
                order
        );
    }

    /**
     * Broadcast the current Phase 9 operational schedule snapshot.
     *
     * Schedule failures are isolated so that an exception here does not
     * prevent the existing admin/user order broadcasts from executing.
     */
    private void sendKitchenScheduleUpdate() {

        try {
            KitchenScheduleSnapshotResponseDTO scheduleSnapshot =
                    orderService.getKitchenScheduleSnapshot();

            messagingTemplate.convertAndSend(
                    KITCHEN_SCHEDULE_TOPIC,
                    scheduleSnapshot
            );

            log.info(
                    "Kitchen schedule broadcasted. totalOrders={}, totalTasks={}",
                    scheduleSnapshot == null
                            ? null
                            : scheduleSnapshot.getTotalOrders(),
                    scheduleSnapshot == null
                            ? null
                            : scheduleSnapshot.getTotalTasks()
            );

        } catch (Exception ex) {
            log.error(
                    "Failed to broadcast kitchen schedule update",
                    ex
            );
        }
    }
}