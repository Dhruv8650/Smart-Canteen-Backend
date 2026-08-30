package com.smartcanteen.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartcanteen.backend.dto.response.OrderResponseDTO;
import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceImplPhase8Test {

    private OrderRepository orderRepository;
    private PriorityService priorityService;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {

        orderRepository = mock(OrderRepository.class);
        priorityService = mock(PriorityService.class);

        orderService = new OrderServiceImpl(
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
                new KitchenTaskDecompositionServiceImpl(),
                new ResourceWorkloadServiceImpl(30),
                new ResourceBottleneckServiceImpl(),
                new ResourceAwareSchedulingServiceImpl(),
                new ResourceTimelineSchedulingServiceImpl()
        );
    }

    @Test
    void estimatedReadyAtRemainsPopulated() {

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

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        assertEquals(1, result.size());
        assertNotNull(
                result.get(0).getEstimatedReadyAt()
        );
    }

    @Test
    void resourceAwareEstimatedReadyAtIsPopulatedForCookedOrders() {

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

        OrderResponseDTO dto =
                orderService
                        .buildKitchenQueueWithETA()
                        .get(0);

        assertNotNull(
                dto.getEstimatedReadyAt()
        );

        assertNotNull(
                dto.getResourceAwareEstimatedReadyAt()
        );

        assertFalse(
                dto.getResourceAwareEstimatedReadyAt()
                        .isAfter(
                                dto.getEstimatedReadyAt()
                        )
        );
    }

    @Test
    void resourceAwareEtaCanDifferFromLegacyEta() {

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

        mockQueue(
                grillOrder,
                fryerOrder
        );

        mockPriority(
                grillOrder,
                20.0
        );

        mockPriority(
                fryerOrder,
                10.0
        );

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        assertTrue(
                result.get(1)
                        .getEstimatedReadyAt()
                        .isAfter(
                                result.get(0)
                                        .getEstimatedReadyAt()
                        )
        );

        assertTrue(
                result.get(1)
                        .getResourceAwareEstimatedReadyAt()
                        .isBefore(
                                result.get(0)
                                        .getResourceAwareEstimatedReadyAt()
                        )
        );
    }

    @Test
    void differentResourcesCanRunConcurrently() {

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

        mockQueue(
                grillOrder,
                fryerOrder
        );

        mockPriority(
                grillOrder,
                20.0
        );

        mockPriority(
                fryerOrder,
                10.0
        );

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        long difference =
                Duration.between(
                        result.get(1)
                                .getResourceAwareEstimatedReadyAt(),
                        result.get(0)
                                .getResourceAwareEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                7,
                difference
        );
    }

    @Test
    void sameResourceRemainsSequential() {

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

        mockQueue(
                first,
                second
        );

        mockPriority(
                first,
                20.0
        );

        mockPriority(
                second,
                10.0
        );

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        long difference =
                Duration.between(
                        result.get(0)
                                .getResourceAwareEstimatedReadyAt(),
                        result.get(1)
                                .getResourceAwareEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                10,
                difference
        );
    }

    @Test
    void multiResourceOrderUsesLatestTaskCompletion() {

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

        OrderResponseDTO dto =
                orderService
                        .buildKitchenQueueWithETA()
                        .get(0);

        long difference =
                Duration.between(
                        dto.getResourceAwareEstimatedReadyAt(),
                        dto.getEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                8,
                difference
        );
    }

    @Test
    void readyMadeOnlyBehaviorRemainsCorrect() {

        Order order = order(
                1L,
                OrderStatus.PENDING,
                FulfillmentType.DINE_IN,
                0,
                readyMadeFood(
                        501L,
                        0
                )
        );

        mockQueue(order);

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        assertTrue(result.isEmpty());
    }

    @Test
    void mixedReadyMadeAndCookedUsesOnlyCookedTasksForResourceAwareEta() {

        Order order = order(
                1L,
                OrderStatus.PENDING,
                FulfillmentType.DINE_IN,
                109,
                cookedFood(
                        501L,
                        10,
                        KitchenResourceType.GRILL
                ),
                readyMadeFood(
                        601L,
                        99
                )
        );

        mockQueue(order);
        mockPriority(order, 10.0);

        OrderResponseDTO dto =
                orderService
                        .buildKitchenQueueWithETA()
                        .get(0);

        long difference =
                Duration.between(
                        dto.getResourceAwareEstimatedReadyAt(),
                        dto.getEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                99,
                difference
        );
    }

    @Test
    void phaseFiveOrderingRemainsUnchanged() {

        Order olderLowerPriority = cookedOrder(
                1L,
                5,
                cookedFood(
                        501L,
                        5,
                        KitchenResourceType.GRILL
                )
        );

        Order newerHigherPriority = cookedOrder(
                2L,
                5,
                cookedFood(
                        601L,
                        5,
                        KitchenResourceType.FRYER
                )
        );

        setField(
                olderLowerPriority,
                "createdAt",
                LocalDateTime.now(
                        ZoneOffset.UTC
                ).minusMinutes(20)
        );

        setField(
                newerHigherPriority,
                "createdAt",
                LocalDateTime.now(
                        ZoneOffset.UTC
                ).minusMinutes(1)
        );

        mockQueue(
                olderLowerPriority,
                newerHigherPriority
        );

        mockPriority(
                olderLowerPriority,
                1.0
        );

        mockPriority(
                newerHigherPriority,
                100.0
        );

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        assertEquals(
                2L,
                result.get(0).getId()
        );

        assertEquals(
                1L,
                result.get(1).getId()
        );
    }

    @Test
    void cancelledOrdersRemainExcluded() {

        when(
                orderRepository.findByStatusesWithDetails(
                        anyList()
                )
        ).thenReturn(List.of());

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        assertTrue(result.isEmpty());
    }

    @Test
    void legacyEtaCalculationRemainsGlobalSequential() {

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

        mockQueue(
                first,
                second
        );

        mockPriority(
                first,
                20.0
        );

        mockPriority(
                second,
                10.0
        );

        List<OrderResponseDTO> result =
                orderService.buildKitchenQueueWithETA();

        long legacyDifference =
                Duration.between(
                        result.get(0)
                                .getEstimatedReadyAt(),
                        result.get(1)
                                .getEstimatedReadyAt()
                ).toMinutes();

        assertEquals(
                8,
                legacyDifference
        );
    }

    @Test
    void dtoContainsNewField() throws Exception {

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

        OrderResponseDTO dto =
                orderService
                        .buildKitchenQueueWithETA()
                        .get(0);

        ObjectMapper mapper =
                new ObjectMapper()
                        .registerModule(
                                new JavaTimeModule()
                        );

        String json =
                mapper.writeValueAsString(dto);

        assertTrue(
                json.contains("estimatedReadyAt")
        );

        assertTrue(
                json.contains(
                        "resourceAwareEstimatedReadyAt"
                )
        );

        assertFalse(
                json.contains(
                        "\"resourceAwareEstimatedReadyAt\":null"
                )
        );
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

        setField(
                order,
                "id",
                id
        );

        setField(
                order,
                "createdAt",
                LocalDateTime.now(
                        ZoneOffset.UTC
                ).minusMinutes(id)
        );

        order.setUser(user(id));
        order.setStatus(status);
        order.setPaymentMethod(
                PaymentMethod.CASH
        );
        order.setOrderType(
                OrderType.PREPARED
        );
        order.setFulfillmentType(
                fulfillmentType
        );
        order.setTotalAmount(
                BigDecimal.valueOf(100)
        );
        order.setTotalPrepTime(
                totalPrepTime
        );

        List<OrderItem> orderItems =
                new ArrayList<>();

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
        user.setEmail(
                "user" + id + "@example.com"
        );
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
                baseFood(
                        id,
                        prepTimeMinutes
                );

        foodItem.setItemType(
                ItemType.COOKED
        );

        foodItem.setRequiredResource(
                resource
        );

        return foodItem;
    }

    private FoodItem readyMadeFood(
            Long id,
            int prepTimeMinutes
    ) {

        FoodItem foodItem =
                baseFood(
                        id,
                        prepTimeMinutes
                );

        foodItem.setItemType(
                ItemType.READY_MADE
        );

        return foodItem;
    }

    private FoodItem baseFood(
            Long id,
            int prepTimeMinutes
    ) {

        FoodItem foodItem =
                new FoodItem();

        setField(
                foodItem,
                "id",
                id
        );

        foodItem.setName(
                "Food " + id
        );

        foodItem.setCategory(
                FoodCategory.MAIN
        );

        foodItem.setPrice(
                BigDecimal.valueOf(50)
        );

        foodItem.setAvailable(true);

        foodItem.setPrepTimeMinutes(
                prepTimeMinutes
        );

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
                            .getDeclaredField(
                                    fieldName
                            );

            field.setAccessible(true);
            field.set(
                    target,
                    value
            );

        } catch (ReflectiveOperationException ex) {

            throw new AssertionError(
                    "Unable to set field: "
                            + fieldName,
                    ex
            );
        }
    }
}