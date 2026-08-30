package com.smartcanteen.backend.service.impl;

import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.response.scheduling.KitchenOrderReadyTimeResponseDTO;
import com.smartcanteen.backend.dto.response.scheduling.KitchenResourceScheduleResponseDTO;
import com.smartcanteen.backend.dto.response.scheduling.KitchenScheduleSnapshotResponseDTO;
import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ResourceTimeline;
import com.smartcanteen.backend.dto.scheduling.ResourceTimelineSnapshot;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.ScheduledResourceTask;
import com.smartcanteen.backend.dto.scheduling.ScheduledTaskTimeline;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.FulfillmentType;
import com.smartcanteen.backend.entity.ItemType;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.entity.OrderStatus;
import com.smartcanteen.backend.entity.OrderType;
import com.smartcanteen.backend.entity.PaymentMethod;
import com.smartcanteen.backend.entity.Role;
import com.smartcanteen.backend.entity.User;
import com.smartcanteen.backend.repository.CartRepository;
import com.smartcanteen.backend.repository.FoodItemRepository;
import com.smartcanteen.backend.repository.OrderRepository;
import com.smartcanteen.backend.repository.UserRepository;
import com.smartcanteen.backend.security.QrSecurityUtil;
import com.smartcanteen.backend.service.CanteenService;
import com.smartcanteen.backend.service.CartService;
import com.smartcanteen.backend.service.PriorityService;
import com.smartcanteen.backend.service.RoutingService;
import com.smartcanteen.backend.service.scheduling.KitchenTaskDecompositionService;
import com.smartcanteen.backend.service.scheduling.ResourceAwareSchedulingService;
import com.smartcanteen.backend.service.scheduling.ResourceBottleneckService;
import com.smartcanteen.backend.service.scheduling.ResourceTimelineSchedulingService;
import com.smartcanteen.backend.service.scheduling.ResourceWorkloadService;
import com.smartcanteen.backend.service.scheduling.impl.KitchenTaskDecompositionServiceImpl;
import com.smartcanteen.backend.service.scheduling.impl.ResourceAwareSchedulingServiceImpl;
import com.smartcanteen.backend.service.scheduling.impl.ResourceBottleneckServiceImpl;
import com.smartcanteen.backend.service.scheduling.impl.ResourceTimelineSchedulingServiceImpl;
import com.smartcanteen.backend.service.scheduling.impl.ResourceWorkloadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplPhase9Test {

    private static final double DELTA = 0.000001;

    private OrderRepository orderRepository;
    private PriorityService priorityService;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {

        orderRepository = mock(OrderRepository.class);
        priorityService = mock(PriorityService.class);

        orderService = orderService(
                priorityService,
                new KitchenTaskDecompositionServiceImpl(),
                new ResourceWorkloadServiceImpl(30),
                new ResourceBottleneckServiceImpl(),
                new ResourceAwareSchedulingServiceImpl(),
                new ResourceTimelineSchedulingServiceImpl()
        );
    }

    @Test
    void getKitchenScheduleSnapshotReturnsScheduledAt() {

        Order order = cookedOrder(
                1L,
                10,
                cookedFood(
                        501L,
                        10,
                        KitchenResourceType.GRILL
                )
        );

        mockQueue(order);
        mockPriority(order, 10.0);

        KitchenScheduleSnapshotResponseDTO result =
                orderService.getKitchenScheduleSnapshot();

        assertNotNull(
                result.getScheduledAt()
        );
    }

    @Test
    void getKitchenScheduleSnapshotExposesResourceWorkloads() {

        Order order = cookedOrder(
                1L,
                15,
                cookedFood(
                        501L,
                        15,
                        KitchenResourceType.GRILL
                )
        );

        mockQueue(order);
        mockPriority(order, 10.0);

        KitchenResourceScheduleResponseDTO grill =
                resource(
                        orderService.getKitchenScheduleSnapshot(),
                        KitchenResourceType.GRILL
                );

        assertEquals(
                15,
                grill.getWorkloadMinutes()
        );

        assertEquals(
                0.5,
                grill.getCongestion(),
                DELTA
        );

        assertEquals(
                0.5,
                grill.getPressure(),
                DELTA
        );
    }

    @Test
    void getKitchenScheduleSnapshotExposesDetectedBottleneck() {

        Order grillOrder = cookedOrder(
                1L,
                35,
                cookedFood(
                        501L,
                        35,
                        KitchenResourceType.GRILL
                )
        );

        Order fryerOrder = cookedOrder(
                2L,
                5,
                cookedFood(
                        601L,
                        5,
                        KitchenResourceType.FRYER
                )
        );

        mockQueue(
                grillOrder,
                fryerOrder
        );

        mockPriority(grillOrder, 20.0);
        mockPriority(fryerOrder, 10.0);

        KitchenScheduleSnapshotResponseDTO result =
                orderService.getKitchenScheduleSnapshot();

        assertNotNull(
                result.getBottleneck()
        );

        assertEquals(
                KitchenResourceType.GRILL,
                result.getBottleneck().getResource()
        );

        assertEquals(
                35,
                result.getBottleneck().getWorkloadMinutes()
        );
    }

    @Test
    void getKitchenScheduleSnapshotExposesDispatchQueues() {

        Order order = cookedOrder(
                1L,
                23,
                cookedFood(
                        501L,
                        15,
                        KitchenResourceType.GRILL
                ),
                cookedFood(
                        601L,
                        8,
                        KitchenResourceType.FRYER
                )
        );

        mockQueue(order);
        mockPriority(order, 10.0);

        KitchenScheduleSnapshotResponseDTO result =
                orderService.getKitchenScheduleSnapshot();

        assertEquals(
                1,
                resource(result, KitchenResourceType.GRILL)
                        .getDispatchQueue()
                        .size()
        );

        assertEquals(
                1,
                resource(result, KitchenResourceType.FRYER)
                        .getDispatchQueue()
                        .size()
        );

        assertEquals(
                1,
                resource(result, KitchenResourceType.GRILL)
                        .getDispatchQueue()
                        .get(0)
                        .getSequence()
        );
    }

    @Test
    void getKitchenScheduleSnapshotExposesTimelines() {

        Order first = cookedOrder(
                1L,
                15,
                cookedFood(
                        501L,
                        15,
                        KitchenResourceType.GRILL
                )
        );

        Order second = cookedOrder(
                2L,
                10,
                cookedFood(
                        601L,
                        10,
                        KitchenResourceType.GRILL
                )
        );

        mockQueue(first, second);
        mockPriority(first, 20.0);
        mockPriority(second, 10.0);

        KitchenResourceScheduleResponseDTO grill =
                resource(
                        orderService.getKitchenScheduleSnapshot(),
                        KitchenResourceType.GRILL
                );

        assertEquals(
                2,
                grill.getTimeline().size()
        );

        assertEquals(
                grill.getTimeline().get(0).getEndTime(),
                grill.getTimeline().get(1).getStartTime()
        );

        assertEquals(
                25,
                grill.getTotalDurationMinutes()
        );
    }

    @Test
    void getKitchenScheduleSnapshotExposesOrderReadyTimes() {

        Order grillOrder = cookedOrder(
                1L,
                15,
                cookedFood(
                        501L,
                        15,
                        KitchenResourceType.GRILL
                )
        );

        Order fryerOrder = cookedOrder(
                2L,
                8,
                cookedFood(
                        601L,
                        8,
                        KitchenResourceType.FRYER
                )
        );

        mockQueue(grillOrder, fryerOrder);
        mockPriority(grillOrder, 20.0);
        mockPriority(fryerOrder, 10.0);

        List<KitchenOrderReadyTimeResponseDTO> readyTimes =
                orderService.getKitchenScheduleSnapshot()
                        .getOrderReadyTimes();

        assertEquals(
                2,
                readyTimes.size()
        );

        assertEquals(
                1L,
                readyTimes.get(0).getOrderId()
        );

        assertEquals(
                2L,
                readyTimes.get(1).getOrderId()
        );

        assertTrue(
                readyTimes.get(1)
                        .getReadyAt()
                        .isBefore(
                                readyTimes.get(0)
                                        .getReadyAt()
                        )
        );
    }

    @Test
    void getKitchenScheduleSnapshotUsesOneCoherentCalculation() {

        KitchenTaskDecompositionService decompositionService =
                mock(KitchenTaskDecompositionService.class);

        ResourceWorkloadService workloadService =
                mock(ResourceWorkloadService.class);

        ResourceBottleneckService bottleneckService =
                mock(ResourceBottleneckService.class);

        ResourceAwareSchedulingService dispatchService =
                mock(ResourceAwareSchedulingService.class);

        ResourceTimelineSchedulingService timelineService =
                mock(ResourceTimelineSchedulingService.class);

        OrderServiceImpl service =
                orderService(
                        priorityService,
                        decompositionService,
                        workloadService,
                        bottleneckService,
                        dispatchService,
                        timelineService
                );

        Order order = cookedOrder(
                1L,
                10,
                cookedFood(
                        501L,
                        10,
                        KitchenResourceType.GRILL
                )
        );

        List<SchedulingTask> tasks =
                List.of(
                        new SchedulingTask(
                                1L,
                                100L,
                                501L,
                                1,
                                KitchenResourceType.GRILL,
                                10
                        )
                );

        Map<KitchenResourceType, ResourceWorkload> workloads =
                workloads(
                        new ResourceWorkload(
                                KitchenResourceType.GRILL,
                                10,
                                10.0 / 30.0,
                                10.0 / 30.0
                        )
                );

        Optional<ResourceBottleneck> bottleneck =
                Optional.of(
                        new ResourceBottleneck(
                                KitchenResourceType.GRILL,
                                10,
                                10.0 / 30.0,
                                10.0 / 30.0
                        )
                );

        ResourceScheduleSnapshot dispatchSnapshot =
                new ResourceScheduleSnapshot(
                        Map.of(
                                KitchenResourceType.GRILL,
                                List.of(
                                        new ScheduledResourceTask(
                                                1L,
                                                100L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                10,
                                                1
                                        )
                                )
                        ),
                        KitchenResourceType.GRILL,
                        1
                );

        LocalDateTime readyAt =
                LocalDateTime.of(
                        2026,
                        8,
                        30,
                        10,
                        10
                );

        ResourceTimelineSnapshot timelineSnapshot =
                new ResourceTimelineSnapshot(
                        Map.of(
                                KitchenResourceType.GRILL,
                                new ResourceTimeline(
                                        KitchenResourceType.GRILL,
                                        List.of(
                                                new ScheduledTaskTimeline(
                                                        1L,
                                                        100L,
                                                        501L,
                                                        KitchenResourceType.GRILL,
                                                        10,
                                                        1,
                                                        readyAt.minusMinutes(10),
                                                        readyAt
                                                )
                                        ),
                                        readyAt
                                )
                        ),
                        Map.of(
                                1L,
                                readyAt
                        ),
                        KitchenResourceType.GRILL,
                        readyAt.minusMinutes(10)
                );

        mockQueue(order);
        when(decompositionService.decompose(order))
                .thenReturn(tasks);
        when(workloadService.calculateWorkload(tasks))
                .thenReturn(workloads);
        when(bottleneckService.detectBottleneck(workloads))
                .thenReturn(bottleneck);
        mockPriority(order, 10.0);
        when(dispatchService.buildDispatchSnapshot(
                List.of(order),
                Map.of(
                        1L,
                        tasks
                ),
                bottleneck
        )).thenReturn(dispatchSnapshot);
        when(timelineService.buildTimeline(
                eq(dispatchSnapshot),
                any(LocalDateTime.class)
        )).thenReturn(timelineSnapshot);

        KitchenScheduleSnapshotResponseDTO result =
                service.getKitchenScheduleSnapshot();

        assertEquals(
                readyAt,
                result.getOrderReadyTimes()
                        .get(0)
                        .getReadyAt()
        );

        verify(decompositionService, times(1))
                .decompose(order);
        verify(workloadService, times(1))
                .calculateWorkload(tasks);
        verify(bottleneckService, times(1))
                .detectBottleneck(workloads);
        verify(priorityService, times(1))
                .calculatePriority(
                        eq(order),
                        anyInt(),
                        eq(tasks),
                        eq(workloads),
                        eq(bottleneck)
                );
        verify(dispatchService, times(1))
                .buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                tasks
                        ),
                        bottleneck
                );
        verify(timelineService, times(1))
                .buildTimeline(
                        eq(dispatchSnapshot),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void emptyKitchenQueueReturnsDeterministicEmptySnapshot() {

        mockQueue();

        KitchenScheduleSnapshotResponseDTO result =
                orderService.getKitchenScheduleSnapshot();

        assertNotNull(
                result.getScheduledAt()
        );
        assertEquals(0, result.getTotalOrders());
        assertEquals(0, result.getTotalTasks());
        assertNull(result.getBottleneck());
        assertTrue(result.getOrderReadyTimes().isEmpty());
        assertEquals(
                KitchenResourceType.values().length,
                result.getResources().size()
        );

        for (KitchenResourceScheduleResponseDTO resource :
                result.getResources()) {

            assertEquals(0, resource.getWorkloadMinutes());
            assertEquals(0.0, resource.getCongestion(), DELTA);
            assertEquals(0.0, resource.getPressure(), DELTA);
            assertEquals(0, resource.getTotalDurationMinutes());
            assertEquals(result.getScheduledAt(), resource.getAvailableAt());
            assertTrue(resource.getDispatchQueue().isEmpty());
            assertTrue(resource.getTimeline().isEmpty());
        }
    }

    @Test
    void phaseEightKitchenQueueEtaBehaviorRemainsUnchanged() {

        Order first = cookedOrder(
                1L,
                15,
                cookedFood(
                        501L,
                        15,
                        KitchenResourceType.GRILL
                )
        );

        Order second = cookedOrder(
                2L,
                8,
                cookedFood(
                        601L,
                        8,
                        KitchenResourceType.FRYER
                )
        );

        mockQueue(first, second);
        mockPriority(first, 20.0);
        mockPriority(second, 10.0);

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        long legacyDifference =
                Duration.between(
                        result.get(0).getEstimatedReadyAt(),
                        result.get(1).getEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                8,
                legacyDifference
        );

        assertFalse(
                result.get(0)
                        .getResourceAwareEstimatedReadyAt()
                        .equals(
                                result.get(1)
                                        .getResourceAwareEstimatedReadyAt()
                        )
        );
    }

    private OrderServiceImpl orderService(
            PriorityService priorityService,
            KitchenTaskDecompositionService decompositionService,
            ResourceWorkloadService workloadService,
            ResourceBottleneckService bottleneckService,
            ResourceAwareSchedulingService dispatchService,
            ResourceTimelineSchedulingService timelineService
    ) {

        return new OrderServiceImpl(
                orderRepository,
                mock(UserRepository.class),
                mock(FoodItemRepository.class),
                mock(CartRepository.class),
                mock(ApplicationEventPublisher.class),
                mock(CanteenService.class),
                mock(QrSecurityUtil.class),
                mock(CartService.class),
                mock(RoutingService.class),
                priorityService,
                decompositionService,
                workloadService,
                bottleneckService,
                dispatchService,
                timelineService
        );
    }

    private KitchenResourceScheduleResponseDTO resource(
            KitchenScheduleSnapshotResponseDTO snapshot,
            KitchenResourceType resource
    ) {

        return snapshot.getResources()
                .stream()
                .filter(candidate ->
                        candidate.getResource() == resource
                )
                .findFirst()
                .orElseThrow();
    }

    private Map<KitchenResourceType, ResourceWorkload> workloads(
            ResourceWorkload... workloads
    ) {

        Map<KitchenResourceType, ResourceWorkload> result =
                new EnumMap<>(
                        KitchenResourceType.class
                );

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            result.put(
                    resource,
                    new ResourceWorkload(
                            resource,
                            0,
                            0.0,
                            0.0
                    )
            );
        }

        for (ResourceWorkload workload : workloads) {
            result.put(
                    workload.resource(),
                    workload
            );
        }

        return result;
    }

    private void mockQueue(Order... orders) {

        when(
                orderRepository.findByStatusesWithDetails(
                        anyList()
                )
        ).thenReturn(
                List.of(orders)
        );
    }

    private void mockPriority(
            Order order,
            double priority
    ) {

        when(
                priorityService.calculatePriority(
                        eq(order),
                        anyInt(),
                        anyList(),
                        anyMap(),
                        org.mockito.ArgumentMatchers
                                .<Optional<ResourceBottleneck>>any()
                )
        ).thenReturn(priority);
    }

    private Order cookedOrder(
            Long id,
            int totalPrepTime,
            FoodItem... foodItems
    ) {

        return order(
                id,
                OrderStatus.PENDING,
                FulfillmentType.DINE_IN,
                totalPrepTime,
                foodItems
        );
    }

    private Order order(
            Long id,
            OrderStatus status,
            FulfillmentType fulfillmentType,
            int totalPrepTime,
            FoodItem... foodItems
    ) {

        Order order = new Order();

        setField(order, "id", id);
        setField(
                order,
                "createdAt",
                LocalDateTime.now(
                        ZoneOffset.UTC
                ).minusMinutes(id)
        );

        order.setUser(user(id));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setOrderType(OrderType.PREPARED);
        order.setFulfillmentType(fulfillmentType);
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setTotalPrepTime(totalPrepTime);

        List<OrderItem> orderItems =
                new java.util.ArrayList<>();

        long orderItemId = id * 100;

        for (FoodItem foodItem : foodItems) {
            OrderItem orderItem =
                    new OrderItem();

            setField(
                    orderItem,
                    "id",
                    orderItemId++
            );

            orderItem.setOrder(order);
            orderItem.setFoodItem(foodItem);
            orderItem.setQuantity(1);

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        return order;
    }

    private User user(Long id) {

        User user = new User();

        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setRole(Role.USER);
        user.setActive(true);

        return user;
    }

    private FoodItem cookedFood(
            Long id,
            int prepTimeMinutes,
            KitchenResourceType resource
    ) {

        FoodItem foodItem =
                new FoodItem();

        setField(foodItem, "id", id);

        foodItem.setName("Food " + id);
        foodItem.setCategory(FoodCategory.MAIN);
        foodItem.setPrice(BigDecimal.valueOf(50));
        foodItem.setAvailable(true);
        foodItem.setPrepTimeMinutes(prepTimeMinutes);
        foodItem.setItemType(ItemType.COOKED);
        foodItem.setRequiredResource(resource);

        return foodItem;
    }

    private void setField(
            Object target,
            String fieldName,
            Object value
    ) {

        try {
            Field field =
                    target.getClass()
                            .getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(target, value);

        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(
                    "Unable to set field: " + fieldName,
                    ex
            );
        }
    }
}
