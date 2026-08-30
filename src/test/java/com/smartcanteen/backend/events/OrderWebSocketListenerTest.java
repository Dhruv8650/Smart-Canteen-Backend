package com.smartcanteen.backend.events;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.UserResponseDTO;
import com.smartcanteen.backend.dto.response.scheduling.KitchenScheduleSnapshotResponseDTO;
import com.smartcanteen.backend.dto.websocket.OrderCreatedEvent;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderWebSocketListenerTest {

    private static final String KITCHEN_ORDERS_TOPIC =
            "/topic/kitchen/orders";

    private static final String KITCHEN_SCHEDULE_TOPIC =
            "/topic/kitchen/schedule";

    private static final String ADMIN_ORDERS_TOPIC =
            "/topic/admin/orders";

    private static final String USER_TOPIC =
            "/topic/user/42";

    private SimpMessagingTemplate messagingTemplate;
    private OrderService orderService;
    private OrderWebSocketListener listener;

    private OrderResponseDTO order;
    private List<OrderResponseDTO> kitchenQueue;
    private KitchenScheduleSnapshotResponseDTO scheduleSnapshot;

    @BeforeEach
    void setUp() {

        messagingTemplate = mock(SimpMessagingTemplate.class);

        orderService = mock(OrderService.class);

        listener = new OrderWebSocketListener(
                messagingTemplate,
                orderService
        );

        order = createOrderResponse();

        kitchenQueue = List.of(order);

        scheduleSnapshot =
                mock(KitchenScheduleSnapshotResponseDTO.class);

        when(orderService.buildKitchenQueueWithETA())
                .thenReturn(kitchenQueue);

        when(orderService.getKitchenScheduleSnapshot())
                .thenReturn(scheduleSnapshot);
    }

    @Test
    void orderCreatedEventBroadcastsKitchenQueue() {

        listener.handleOrderCreatedEvent(
                new OrderCreatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_ORDERS_TOPIC,
                kitchenQueue
        );
    }

    @Test
    void orderCreatedEventBroadcastsKitchenScheduleSnapshot() {

        listener.handleOrderCreatedEvent(
                new OrderCreatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_SCHEDULE_TOPIC,
                scheduleSnapshot
        );
    }

    @Test
    void orderStatusUpdatedEventBroadcastsKitchenQueue() {

        listener.handleOrderStatusUpdatedEvent(
                new OrderStatusUpdatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_ORDERS_TOPIC,
                kitchenQueue
        );
    }

    @Test
    void orderStatusUpdatedEventBroadcastsKitchenScheduleSnapshot() {

        listener.handleOrderStatusUpdatedEvent(
                new OrderStatusUpdatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_SCHEDULE_TOPIC,
                scheduleSnapshot
        );
    }

    @Test
    void existingAdminAndUserBroadcastsRemainIntact() {

        listener.handleOrderCreatedEvent(
                new OrderCreatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                ADMIN_ORDERS_TOPIC,
                order
        );

        verify(messagingTemplate).convertAndSend(
                USER_TOPIC,
                order
        );
    }

    @Test
    void scheduleSnapshotFailureDoesNotPreventExistingBroadcasts() {

        when(orderService.getKitchenScheduleSnapshot())
                .thenThrow(new RuntimeException("schedule failed"));

        assertDoesNotThrow(() ->
                listener.handleOrderCreatedEvent(
                        new OrderCreatedEvent(order)
                )
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_ORDERS_TOPIC,
                kitchenQueue
        );

        verify(messagingTemplate, never()).convertAndSend(
                KITCHEN_SCHEDULE_TOPIC,
                scheduleSnapshot
        );

        verify(messagingTemplate).convertAndSend(
                ADMIN_ORDERS_TOPIC,
                order
        );

        verify(messagingTemplate).convertAndSend(
                USER_TOPIC,
                order
        );
    }

    @Test
    void scheduleMessagingFailureDoesNotPreventExistingBroadcasts() {

        doThrow(new RuntimeException("send failed"))
                .when(messagingTemplate)
                .convertAndSend(
                        KITCHEN_SCHEDULE_TOPIC,
                        scheduleSnapshot
                );

        assertDoesNotThrow(() ->
                listener.handleOrderStatusUpdatedEvent(
                        new OrderStatusUpdatedEvent(order)
                )
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_ORDERS_TOPIC,
                kitchenQueue
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_SCHEDULE_TOPIC,
                scheduleSnapshot
        );

        verify(messagingTemplate).convertAndSend(
                ADMIN_ORDERS_TOPIC,
                order
        );

        verify(messagingTemplate).convertAndSend(
                USER_TOPIC,
                order
        );
    }

    @Test
    void schedulePayloadUsesKitchenScheduleSnapshotResponseDto() {

        listener.handleOrderCreatedEvent(
                new OrderCreatedEvent(order)
        );

        verify(messagingTemplate).convertAndSend(
                KITCHEN_SCHEDULE_TOPIC,
                scheduleSnapshot
        );
    }

    private OrderResponseDTO createOrderResponse() {

        UserResponseDTO user = mock(UserResponseDTO.class);
        when(user.getId()).thenReturn(42L);

        return new OrderResponseDTO(
                1L,
                user,
                List.of(),
                BigDecimal.valueOf(100),
                OrderStatus.PENDING,
                LocalDateTime.now(),
                "PREPARED",
                "DINE_IN",
                "ORD-1",
                "Pending",
                "Today",
                1,
                "Test order",
                false,
                false,
                0L,
                "ON_TIME",
                null,
                false,
                1.0,
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusMinutes(10),
                1
        );
    }
}