package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ScheduledResourceTask;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.service.scheduling.impl.ResourceAwareSchedulingServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceAwareSchedulingServiceTest {

    private final ResourceAwareSchedulingService service =
            new ResourceAwareSchedulingServiceImpl();

    @Test
    void preservesCallerSuppliedPhaseFiveOrder() {
        Order first = order(1L);
        Order second = order(2L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(first, second),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                ),
                                2L,
                                List.of(
                                        task(
                                                2L,
                                                201L,
                                                601L,
                                                KitchenResourceType.GRILL,
                                                6
                                        )
                                )
                        ),
                        Optional.empty()
                );

        List<ScheduledResourceTask> grillTasks =
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL);

        assertEquals(
                1L,
                grillTasks.get(0).orderId()
        );

        assertEquals(
                2L,
                grillTasks.get(1).orderId()
        );
    }

    @Test
    void doesNotSortOrdersByOrderPriorityScore() {
        Order first = order(1L);
        Order second = order(2L);

        first.setPriorityScore(1.0);
        second.setPriorityScore(999.0);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(first, second),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                ),
                                2L,
                                List.of(
                                        task(
                                                2L,
                                                201L,
                                                601L,
                                                KitchenResourceType.GRILL,
                                                6
                                        )
                                )
                        ),
                        Optional.empty()
                );

        List<ScheduledResourceTask> grillTasks =
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL);

        assertEquals(
                1L,
                grillTasks.get(0).orderId()
        );

        assertEquals(
                2L,
                grillTasks.get(1).orderId()
        );
    }

    @Test
    void singleResourceTaskIsAddedToMatchingQueue() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                )
                        ),
                        Optional.empty()
                );

        List<ScheduledResourceTask> grillTasks =
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL);

        assertEquals(1, grillTasks.size());
        assertEquals(
                KitchenResourceType.GRILL,
                grillTasks.get(0).resource()
        );
        assertEquals(
                8,
                grillTasks.get(0).durationMinutes()
        );
        assertEquals(
                1,
                grillTasks.get(0).sequence()
        );
        assertEquals(
                1,
                snapshot.totalTasks()
        );
    }

    @Test
    void multipleTasksOnSameResourceUseSameQueueSequence() {
        Order orderA = order(1L);
        Order orderB = order(2L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(orderA, orderB),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                ),
                                2L,
                                List.of(
                                        task(
                                                2L,
                                                201L,
                                                601L,
                                                KitchenResourceType.GRILL,
                                                12
                                        )
                                )
                        ),
                        Optional.empty()
                );

        List<ScheduledResourceTask> grillTasks =
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL);

        assertEquals(2, grillTasks.size());
        assertEquals(1, grillTasks.get(0).sequence());
        assertEquals(2, grillTasks.get(1).sequence());
    }

    @Test
    void differentResourcesUseSeparateQueues() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        ),
                                        task(
                                                1L,
                                                102L,
                                                502L,
                                                KitchenResourceType.FRYER,
                                                4
                                        )
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                1,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL)
                        .size()
        );

        assertEquals(
                1,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.FRYER)
                        .size()
        );
    }

    @Test
    void multiResourceOrderAppearsInEachRequiredResourceQueue() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                15
                                        ),
                                        task(
                                                1L,
                                                102L,
                                                502L,
                                                KitchenResourceType.FRYER,
                                                8
                                        )
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                1L,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL)
                        .get(0)
                        .orderId()
        );

        assertEquals(
                1L,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.FRYER)
                        .get(0)
                        .orderId()
        );
    }

    @Test
    void preservesTaskOrderWithinEachOrder() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                102L,
                                                502L,
                                                KitchenResourceType.GRILL,
                                                8
                                        ),
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                )
                        ),
                        Optional.empty()
                );

        List<ScheduledResourceTask> grillTasks =
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL);

        assertEquals(
                102L,
                grillTasks.get(0).orderItemId()
        );

        assertEquals(
                101L,
                grillTasks.get(1).orderItemId()
        );
    }

    @Test
    void bottleneckMetadataIsPreservedButDoesNotReorderAnything() {
        Order grillOrder = order(1L);
        Order fryerOrder = order(2L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(fryerOrder, grillOrder),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8
                                        )
                                ),
                                2L,
                                List.of(
                                        task(
                                                2L,
                                                201L,
                                                601L,
                                                KitchenResourceType.FRYER,
                                                8
                                        )
                                )
                        ),
                        Optional.of(
                                new ResourceBottleneck(
                                        KitchenResourceType.GRILL,
                                        30,
                                        1.0,
                                        1.0
                                )
                        )
                );

        assertEquals(
                KitchenResourceType.GRILL,
                snapshot.bottleneckResource()
        );

        assertEquals(
                2L,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.FRYER)
                        .get(0)
                        .orderId()
        );

        assertEquals(
                1L,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL)
                        .get(0)
                        .orderId()
        );
    }

    @Test
    void readyMadeOnlyOrderProducesNoTasksWhenTaskListIsEmpty() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of()
                        ),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );

        assertAllResourceQueuesEmpty(snapshot);
    }

    @Test
    void nullTaskIsIgnored() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        mapWithTasks(
                                1L,
                                null,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                1,
                snapshot.totalTasks()
        );
    }

    @Test
    void nullResourceTaskIsIgnored() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                null,
                                                8
                                        )
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );
    }

    @Test
    void zeroDurationTaskIsIgnored() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                0
                                        )
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );
    }

    @Test
    void negativeDurationTaskIsIgnored() {
        Order order = order(1L);

        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order),
                        Map.of(
                                1L,
                                List.of(
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                -1
                                        )
                                )
                        ),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );
    }

    @Test
    void emptyOrdersProduceEmptySnapshot() {
        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(),
                        Map.of(),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );

        assertAllResourceQueuesEmpty(snapshot);
    }

    @Test
    void nullOrdersProduceEmptySnapshot() {
        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        null,
                        Map.of(),
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );

        assertAllResourceQueuesEmpty(snapshot);
    }

    @Test
    void nullTasksMapProducesEmptySnapshot() {
        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(order(1L)),
                        null,
                        Optional.empty()
                );

        assertEquals(
                0,
                snapshot.totalTasks()
        );

        assertAllResourceQueuesEmpty(snapshot);
    }

    @Test
    void allResourcesAreRepresentedDeterministically() {
        ResourceScheduleSnapshot snapshot =
                service.buildDispatchSnapshot(
                        List.of(),
                        Map.of(),
                        Optional.empty()
                );

        assertEquals(
                KitchenResourceType.values().length,
                snapshot.tasksByResource().size()
        );

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            assertTrue(
                    snapshot.tasksByResource()
                            .containsKey(resource)
            );
        }
    }

    @Test
    void repeatedSchedulingProducesIdenticalSnapshot() {
        Order order = order(1L);

        Map<Long, List<SchedulingTask>> tasksByOrder =
                Map.of(
                        1L,
                        List.of(
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8
                                )
                        )
                );

        ResourceScheduleSnapshot first =
                service.buildDispatchSnapshot(
                        List.of(order),
                        tasksByOrder,
                        Optional.empty()
                );

        ResourceScheduleSnapshot second =
                service.buildDispatchSnapshot(
                        List.of(order),
                        tasksByOrder,
                        Optional.empty()
                );

        assertEquals(
                first,
                second
        );
    }

    @Test
    void inputCollectionsAreNotMutated() {
        Order order = order(1L);

        List<Order> orders =
                new ArrayList<>(
                        List.of(order)
                );

        List<SchedulingTask> tasks =
                new ArrayList<>(
                        List.of(
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8
                                )
                        )
                );

        Map<Long, List<SchedulingTask>> tasksByOrder =
                new HashMap<>();

        tasksByOrder.put(
                1L,
                tasks
        );

        service.buildDispatchSnapshot(
                orders,
                tasksByOrder,
                Optional.empty()
        );

        assertEquals(
                1,
                orders.size()
        );

        assertEquals(
                1,
                tasks.size()
        );

        assertEquals(
                1,
                tasksByOrder.size()
        );
    }

    private Order order(Long id) {
        Order order = new Order();

        setField(
                order,
                "id",
                id
        );

        return order;
    }

    private SchedulingTask task(
            Long orderId,
            Long orderItemId,
            Long foodItemId,
            KitchenResourceType resource,
            int durationMinutes
    ) {
        return new SchedulingTask(
                orderId,
                orderItemId,
                foodItemId,
                1,
                resource,
                durationMinutes
        );
    }

    private Map<Long, List<SchedulingTask>> mapWithTasks(
            Long orderId,
            SchedulingTask... tasks
    ) {
        Map<Long, List<SchedulingTask>> result =
                new HashMap<>();

        result.put(
                orderId,
                Arrays.asList(tasks)
        );

        return result;
    }

    private void assertAllResourceQueuesEmpty(
            ResourceScheduleSnapshot snapshot
    ) {
        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            assertTrue(
                    snapshot.tasksByResource()
                            .get(resource)
                            .isEmpty()
            );
        }
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

            field.set(
                    target,
                    value
            );

        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(
                    "Unable to set field: " + fieldName,
                    ex
            );
        }
    }
}