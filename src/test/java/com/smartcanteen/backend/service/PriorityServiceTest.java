package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityServiceTest {

    private static final double DELTA = 0.000001;

    private final PriorityService service =
            new PriorityService(
                    1.0,
                    0.5,
                    1.5
            );

    @Test
    void oldCalculatePriorityPathRemainsUnchangedForLowQueueLoad() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        double priority =
                service.calculatePriority(
                        order,
                        5
                );

        assertEquals(
                0.1,
                priority,
                DELTA
        );
    }

    @Test
    void oldCalculatePriorityPathRemainsUnchangedForHighQueueLoad() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        double priority =
                service.calculatePriority(
                        order,
                        10
                );

        assertEquals(
                0.2,
                priority,
                DELTA
        );
    }

    @Test
    void oldCalculatePriorityStillAppliesPreparingStatusBoost() {

        Order order = order(
                OrderStatus.PREPARING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        double priority =
                service.calculatePriority(
                        order,
                        5
                );

        assertEquals(
                0.6,
                priority,
                DELTA
        );
    }

    @Test
    void resourceAdjustmentIsAddedToBasePriority() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<SchedulingTask> tasks =
                List.of(
                        task(
                                KitchenResourceType.GRILL,
                                10
                        )
                );

        Map<KitchenResourceType, ResourceWorkload>
                workloads =
                workloads(
                        workload(
                                KitchenResourceType.GRILL,
                                24,
                                0.8,
                                0.8
                        )
                );

        Optional<ResourceBottleneck> bottleneck =
                Optional.of(
                        new ResourceBottleneck(
                                KitchenResourceType.GRILL,
                                24,
                                0.8,
                                0.8
                        )
                );

        double priority =
                service.calculatePriority(
                        order,
                        5,
                        tasks,
                        workloads,
                        bottleneck
                );

        double basePriority =
                service.calculatePriority(
                        order,
                        5
                );

        assertEquals(
                basePriority + 1.2,
                priority,
                DELTA
        );
    }

    @Test
    void resourceAdjustmentIsCapped() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<SchedulingTask> tasks =
                List.of(
                        task(
                                KitchenResourceType.GRILL,
                                10
                        )
                );

        Map<KitchenResourceType, ResourceWorkload>
                workloads =
                workloads(
                        workload(
                                KitchenResourceType.GRILL,
                                60,
                                1.0,
                                1.0
                        )
                );

        Optional<ResourceBottleneck> bottleneck =
                Optional.of(
                        new ResourceBottleneck(
                                KitchenResourceType.GRILL,
                                60,
                                1.0,
                                1.0
                        )
                );

        double priority =
                service.calculatePriority(
                        order,
                        5,
                        tasks,
                        workloads,
                        bottleneck
                );

        double basePriority =
                service.calculatePriority(
                        order,
                        5
                );

        assertEquals(
                basePriority + 1.5,
                priority,
                DELTA
        );
    }

    @Test
    void emptyTasksUseOnlyBasePriority() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        double priority =
                service.calculatePriority(
                        order,
                        5,
                        List.of(),
                        workloads(),
                        Optional.empty()
                );

        assertEquals(
                service.calculatePriority(
                        order,
                        5
                ),
                priority,
                DELTA
        );
    }

    @Test
    void missingWorkloadUsesZeroPressureForThatTask() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        double priority =
                service.calculatePriority(
                        order,
                        5,
                        List.of(
                                task(
                                        KitchenResourceType.GRILL,
                                        10
                                )
                        ),
                        workloads(),
                        Optional.empty()
                );

        assertEquals(
                service.calculatePriority(
                        order,
                        5
                ),
                priority,
                DELTA
        );
    }

    @Test
    void weightedResourcePressureUsesTaskDurations() {

        Order order = order(
                OrderStatus.PENDING,
                10,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        List<SchedulingTask> tasks =
                List.of(
                        task(
                                KitchenResourceType.GRILL,
                                10
                        ),
                        task(
                                KitchenResourceType.FRYER,
                                30
                        )
                );

        Map<KitchenResourceType, ResourceWorkload>
                workloads =
                workloads(
                        workload(
                                KitchenResourceType.GRILL,
                                30,
                                1.0,
                                1.0
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                15,
                                0.5,
                                0.5
                        )
                );

        double priority =
                service.calculatePriority(
                        order,
                        5,
                        tasks,
                        workloads,
                        Optional.empty()
                );

        double basePriority =
                service.calculatePriority(
                        order,
                        5
                );

        double expectedPressure =
                ((10 * 1.0) + (30 * 0.5))
                        / 40.0;

        assertEquals(
                basePriority + expectedPressure,
                priority,
                DELTA
        );
    }

    private Order order(
            OrderStatus status,
            Integer totalPrepTime,
            LocalDateTime createdAt
    ) {

        Order order = new Order();

        order.setStatus(status);
        order.setTotalPrepTime(totalPrepTime);

        setCreatedAt(
                order,
                createdAt
        );

        return order;
    }

    private void setCreatedAt(
            Order order,
            LocalDateTime createdAt
    ) {

        try {

            Field field =
                    Order.class.getDeclaredField(
                            "createdAt"
                    );

            field.setAccessible(true);

            field.set(
                    order,
                    createdAt
            );

        } catch (ReflectiveOperationException ex) {

            throw new AssertionError(
                    "Unable to set Order.createdAt for unit test",
                    ex
            );
        }
    }

    private SchedulingTask task(
            KitchenResourceType resource,
            int durationMinutes
    ) {

        return new SchedulingTask(
                1L,
                1L,
                1L,
                1,
                resource,
                durationMinutes
        );
    }

    private ResourceWorkload workload(
            KitchenResourceType resource,
            long workloadMinutes,
            double congestion,
            double pressure
    ) {

        return new ResourceWorkload(
                resource,
                workloadMinutes,
                congestion,
                pressure
        );
    }

    private Map<KitchenResourceType, ResourceWorkload>
    workloads(
            ResourceWorkload... workloads
    ) {

        Map<KitchenResourceType, ResourceWorkload>
                result =
                new EnumMap<>(
                        KitchenResourceType.class
                );

        for (ResourceWorkload workload : workloads) {

            result.put(
                    workload.resource(),
                    workload
            );
        }

        return result;
    }
}