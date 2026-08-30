package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ResourceTimelineSnapshot;
import com.smartcanteen.backend.dto.scheduling.ScheduledResourceTask;
import com.smartcanteen.backend.dto.scheduling.ScheduledTaskTimeline;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.impl.ResourceTimelineSchedulingServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTimelineSchedulingServiceTest {

    private final ResourceTimelineSchedulingService service =
            new ResourceTimelineSchedulingServiceImpl();

    private final LocalDateTime schedulingTime =
            LocalDateTime.of(
                    2026,
                    8,
                    30,
                    10,
                    0
            );

    @Test
    void singleGrillTaskStartsAtSchedulingTime() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        15,
                                        1
                                )
                        ),
                        schedulingTime
                );

        ScheduledTaskTimeline scheduledTask =
                tasks(
                        result,
                        KitchenResourceType.GRILL
                ).get(0);

        assertEquals(
                schedulingTime,
                scheduledTask.startTime()
        );

        assertEquals(
                schedulingTime.plusMinutes(15),
                scheduledTask.endTime()
        );
    }

    @Test
    void twoGrillTasksAreSequential() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        15,
                                        1
                                ),
                                task(
                                        2L,
                                        201L,
                                        601L,
                                        KitchenResourceType.GRILL,
                                        10,
                                        2
                                )
                        ),
                        schedulingTime
                );

        List<ScheduledTaskTimeline> tasks =
                tasks(
                        result,
                        KitchenResourceType.GRILL
                );

        assertEquals(
                tasks.get(0).endTime(),
                tasks.get(1).startTime()
        );

        assertEquals(
                schedulingTime.plusMinutes(25),
                tasks.get(1).endTime()
        );
    }

    @Test
    void differentResourcesCanStartTogether() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                Map.of(
                                        KitchenResourceType.GRILL,
                                        List.of(
                                                task(
                                                        1L,
                                                        101L,
                                                        501L,
                                                        KitchenResourceType.GRILL,
                                                        15,
                                                        1
                                                )
                                        ),
                                        KitchenResourceType.FRYER,
                                        List.of(
                                                task(
                                                        2L,
                                                        201L,
                                                        601L,
                                                        KitchenResourceType.FRYER,
                                                        8,
                                                        1
                                                )
                                        )
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                schedulingTime,
                tasks(
                        result,
                        KitchenResourceType.GRILL
                ).get(0).startTime()
        );

        assertEquals(
                schedulingTime,
                tasks(
                        result,
                        KitchenResourceType.FRYER
                ).get(0).startTime()
        );
    }

    @Test
    void multiResourceOrderUsesLatestCompletionTime() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                Map.of(
                                        KitchenResourceType.GRILL,
                                        List.of(
                                                task(
                                                        100L,
                                                        101L,
                                                        501L,
                                                        KitchenResourceType.GRILL,
                                                        15,
                                                        1
                                                )
                                        ),
                                        KitchenResourceType.FRYER,
                                        List.of(
                                                task(
                                                        100L,
                                                        102L,
                                                        502L,
                                                        KitchenResourceType.FRYER,
                                                        8,
                                                        1
                                                )
                                        )
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                schedulingTime.plusMinutes(15),
                result.orderReadyAt().get(100L)
        );
    }

    @Test
    void phaseSixOrderIsPreserved() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        10L,
                                        1001L,
                                        5001L,
                                        KitchenResourceType.GRILL,
                                        30,
                                        1
                                ),
                                task(
                                        5L,
                                        501L,
                                        2501L,
                                        KitchenResourceType.GRILL,
                                        1,
                                        2
                                )
                        ),
                        schedulingTime
                );

        List<ScheduledTaskTimeline> tasks =
                tasks(
                        result,
                        KitchenResourceType.GRILL
                );

        assertEquals(
                10L,
                tasks.get(0).orderId()
        );

        assertEquals(
                5L,
                tasks.get(1).orderId()
        );
    }

    @Test
    void bottleneckDoesNotReorderTasks() {

        ResourceScheduleSnapshot snapshot =
                new ResourceScheduleSnapshot(
                        Map.of(
                                KitchenResourceType.GRILL,
                                List.of(
                                        task(
                                                2L,
                                                201L,
                                                601L,
                                                KitchenResourceType.GRILL,
                                                8,
                                                1
                                        ),
                                        task(
                                                1L,
                                                101L,
                                                501L,
                                                KitchenResourceType.GRILL,
                                                8,
                                                2
                                        )
                                )
                        ),
                        KitchenResourceType.GRILL,
                        2
                );

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot,
                        schedulingTime
                );

        List<ScheduledTaskTimeline> tasks =
                tasks(
                        result,
                        KitchenResourceType.GRILL
                );

        assertEquals(
                2L,
                tasks.get(0).orderId()
        );

        assertEquals(
                1L,
                tasks.get(1).orderId()
        );
    }

    @Test
    void bottleneckMetadataIsPreserved() {

        ResourceScheduleSnapshot snapshot =
                new ResourceScheduleSnapshot(
                        Map.of(),
                        KitchenResourceType.GRILL,
                        0
                );

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot,
                        schedulingTime
                );

        assertEquals(
                KitchenResourceType.GRILL,
                result.bottleneckResource()
        );
    }

    @Test
    void nullSnapshotProducesEmptyTimeline() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        null,
                        schedulingTime
                );

        assertEquals(
                0,
                result.totalScheduledTasks()
        );

        assertAllResourcesEmpty(result);
    }

    @Test
    void nullTaskIsIgnoredWhileValidTasksAreScheduled() {

        List<ScheduledResourceTask> tasks =
                new ArrayList<>();

        tasks.add(null);

        tasks.add(
                task(
                        1L,
                        101L,
                        501L,
                        KitchenResourceType.GRILL,
                        8,
                        1
                )
        );

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        new ResourceScheduleSnapshot(
                                Map.of(
                                        KitchenResourceType.GRILL,
                                        tasks
                                ),
                                null,
                                1
                        ),
                        schedulingTime
                );

        assertEquals(
                1,
                result.totalScheduledTasks()
        );
    }

    @Test
    void nullResourceTaskIsIgnored() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        null,
                                        8,
                                        1
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                0,
                result.totalScheduledTasks()
        );
    }

    @Test
    void zeroDurationTaskIsIgnored() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        0,
                                        1
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                0,
                result.totalScheduledTasks()
        );
    }

    @Test
    void negativeDurationTaskIsIgnored() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        -5,
                                        1
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                0,
                result.totalScheduledTasks()
        );
    }

    @Test
    void nullOrderIdIsScheduledWithoutOrderReadyEntry() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        null,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8,
                                        1
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                1,
                result.totalScheduledTasks()
        );

        assertTrue(
                result.orderReadyAt().isEmpty()
        );
    }

    @Test
    void threeResourcesForSameOrderUseLatestEndTime() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                Map.of(
                                        KitchenResourceType.GRILL,
                                        List.of(
                                                task(
                                                        1L,
                                                        101L,
                                                        501L,
                                                        KitchenResourceType.GRILL,
                                                        7,
                                                        1
                                                )
                                        ),
                                        KitchenResourceType.FRYER,
                                        List.of(
                                                task(
                                                        1L,
                                                        102L,
                                                        502L,
                                                        KitchenResourceType.FRYER,
                                                        9,
                                                        1
                                                )
                                        ),
                                        KitchenResourceType.BEVERAGE,
                                        List.of(
                                                task(
                                                        1L,
                                                        103L,
                                                        503L,
                                                        KitchenResourceType.BEVERAGE,
                                                        2,
                                                        1
                                                )
                                        )
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                schedulingTime.plusMinutes(9),
                result.orderReadyAt().get(1L)
        );
    }

    @Test
    void resourceAvailableAtEqualsFinalTaskEndTime() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8,
                                        1
                                ),
                                task(
                                        2L,
                                        201L,
                                        601L,
                                        KitchenResourceType.GRILL,
                                        7,
                                        2
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                schedulingTime.plusMinutes(15),
                result.timelinesByResource()
                        .get(KitchenResourceType.GRILL)
                        .availableAt()
        );
    }

    @Test
    void resourceTotalDurationIsCorrect() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                KitchenResourceType.GRILL,
                                task(
                                        1L,
                                        101L,
                                        501L,
                                        KitchenResourceType.GRILL,
                                        8,
                                        1
                                ),
                                task(
                                        2L,
                                        201L,
                                        601L,
                                        KitchenResourceType.GRILL,
                                        7,
                                        2
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                15,
                result.timelinesByResource()
                        .get(KitchenResourceType.GRILL)
                        .totalDurationMinutes()
        );
    }

    @Test
    void totalScheduledTasksIsCorrect() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        snapshot(
                                Map.of(
                                        KitchenResourceType.GRILL,
                                        List.of(
                                                task(
                                                        1L,
                                                        101L,
                                                        501L,
                                                        KitchenResourceType.GRILL,
                                                        8,
                                                        1
                                                )
                                        ),
                                        KitchenResourceType.FRYER,
                                        List.of(
                                                task(
                                                        2L,
                                                        201L,
                                                        601L,
                                                        KitchenResourceType.FRYER,
                                                        7,
                                                        1
                                                )
                                        )
                                )
                        ),
                        schedulingTime
                );

        assertEquals(
                2,
                result.totalScheduledTasks()
        );
    }

    @Test
    void repeatedExecutionIsDeterministic() {

        ResourceScheduleSnapshot snapshot =
                snapshot(
                        KitchenResourceType.GRILL,
                        task(
                                1L,
                                101L,
                                501L,
                                KitchenResourceType.GRILL,
                                8,
                                1
                        )
                );

        ResourceTimelineSnapshot first =
                service.buildTimeline(
                        snapshot,
                        schedulingTime
                );

        ResourceTimelineSnapshot second =
                service.buildTimeline(
                        snapshot,
                        schedulingTime
                );

        assertEquals(
                first,
                second
        );
    }

    @Test
    void inputSnapshotIsNotModified() {

        ResourceScheduleSnapshot snapshot =
                snapshot(
                        KitchenResourceType.GRILL,
                        task(
                                1L,
                                101L,
                                501L,
                                KitchenResourceType.GRILL,
                                8,
                                1
                        )
                );

        service.buildTimeline(
                snapshot,
                schedulingTime
        );

        assertEquals(
                1,
                snapshot.totalTasks()
        );

        assertEquals(
                1,
                snapshot.tasksByResource()
                        .get(KitchenResourceType.GRILL)
                        .size()
        );
    }

    @Test
    void allResourcesAreRepresented() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        null,
                        schedulingTime
                );

        assertEquals(
                KitchenResourceType.values().length,
                result.timelinesByResource().size()
        );

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            assertTrue(
                    result.timelinesByResource()
                            .containsKey(resource)
            );
        }
    }

    @Test
    void emptyResourceAvailableAtEqualsSchedulingTime() {

        ResourceTimelineSnapshot result =
                service.buildTimeline(
                        null,
                        schedulingTime
                );

        assertEquals(
                schedulingTime,
                result.timelinesByResource()
                        .get(KitchenResourceType.GRILL)
                        .availableAt()
        );
    }

    private List<ScheduledTaskTimeline> tasks(
            ResourceTimelineSnapshot snapshot,
            KitchenResourceType resource
    ) {
        return snapshot.timelinesByResource()
                .get(resource)
                .tasks();
    }

    private ResourceScheduleSnapshot snapshot(
            KitchenResourceType resource,
            ScheduledResourceTask... tasks
    ) {
        return snapshot(
                Map.of(
                        resource,
                        List.of(tasks)
                )
        );
    }

    private ResourceScheduleSnapshot snapshot(
            Map<KitchenResourceType,
                    List<ScheduledResourceTask>> tasksByResource
    ) {
        return new ResourceScheduleSnapshot(
                tasksByResource,
                null,
                0
        );
    }

    private ScheduledResourceTask task(
            Long orderId,
            Long orderItemId,
            Long foodItemId,
            KitchenResourceType resource,
            int durationMinutes,
            int sequence
    ) {
        return new ScheduledResourceTask(
                orderId,
                orderItemId,
                foodItemId,
                resource,
                durationMinutes,
                sequence
        );
    }

    private void assertAllResourcesEmpty(
            ResourceTimelineSnapshot snapshot
    ) {
        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            assertTrue(
                    snapshot.timelinesByResource()
                            .get(resource)
                            .tasks()
                            .isEmpty()
            );
        }
    }
}