package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.impl.ResourceWorkloadServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceWorkloadServiceTest {

    private static final double DELTA = 0.000001;

    private final ResourceWorkloadService service =
            new ResourceWorkloadServiceImpl(30);

    @Test
    void emptyTaskListReturnsAllResourcesWithZeroWorkload() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of());

        assertAllResourcesPresent(result);

        assertZero(result, KitchenResourceType.GRILL);
        assertZero(result, KitchenResourceType.FRYER);
        assertZero(result, KitchenResourceType.BEVERAGE);
        assertZero(result, KitchenResourceType.PREPARATION);
    }

    @Test
    void nullTaskListReturnsAllResourcesWithZeroWorkload() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(null);

        assertAllResourcesPresent(result);

        assertZero(result, KitchenResourceType.GRILL);
        assertZero(result, KitchenResourceType.FRYER);
        assertZero(result, KitchenResourceType.BEVERAGE);
        assertZero(result, KitchenResourceType.PREPARATION);
    }

    @Test
    void oneGrillTaskAddsGrillWorkload() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 8)
                ));

        assertEquals(
                8L,
                result.get(KitchenResourceType.GRILL)
                        .workloadMinutes()
        );

        assertEquals(
                8.0 / 30.0,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void multipleGrillTasksAreSummed() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 8),
                        task(KitchenResourceType.GRILL, 12),
                        task(KitchenResourceType.GRILL, 5)
                ));

        assertEquals(
                25L,
                result.get(KitchenResourceType.GRILL)
                        .workloadMinutes()
        );
    }

    @Test
    void multipleResourcesAreAggregatedIndependently() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 8),
                        task(KitchenResourceType.GRILL, 12),
                        task(KitchenResourceType.FRYER, 5),
                        task(KitchenResourceType.BEVERAGE, 3),
                        task(KitchenResourceType.PREPARATION, 7)
                ));

        assertEquals(
                20L,
                result.get(KitchenResourceType.GRILL)
                        .workloadMinutes()
        );

        assertEquals(
                5L,
                result.get(KitchenResourceType.FRYER)
                        .workloadMinutes()
        );

        assertEquals(
                3L,
                result.get(KitchenResourceType.BEVERAGE)
                        .workloadMinutes()
        );

        assertEquals(
                7L,
                result.get(KitchenResourceType.PREPARATION)
                        .workloadMinutes()
        );
    }

    @Test
    void resourceWithNoTasksStillExistsWithZeroWorkload() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 8)
                ));

        assertEquals(
                0L,
                result.get(KitchenResourceType.FRYER)
                        .workloadMinutes()
        );

        assertEquals(
                0.0,
                result.get(KitchenResourceType.FRYER)
                        .congestion(),
                DELTA
        );

        assertEquals(
                0.0,
                result.get(KitchenResourceType.FRYER)
                        .pressure(),
                DELTA
        );
    }

    @Test
    void congestionIsZeroForZeroWorkload() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of());

        assertEquals(
                0.0,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void congestionIsFiftyPercentForHalfPlanningWindow() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 15)
                ));

        assertEquals(
                0.5,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void congestionIsOneHundredPercentForFullPlanningWindow() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 30)
                ));

        assertEquals(
                1.0,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void congestionAboveOneHundredPercentIsClamped() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 60)
                ));

        assertEquals(
                1.0,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void pressureEqualsCongestion() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 15)
                ));

        ResourceWorkload workload =
                result.get(KitchenResourceType.GRILL);

        assertEquals(
                workload.congestion(),
                workload.pressure(),
                DELTA
        );
    }

    @Test
    void nullResourceTaskIsIgnored() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(null, 10)
                ));

        assertZero(result, KitchenResourceType.GRILL);
        assertZero(result, KitchenResourceType.FRYER);
        assertZero(result, KitchenResourceType.BEVERAGE);
        assertZero(result, KitchenResourceType.PREPARATION);
    }

    @Test
    void nullTaskIsIgnored() {

        List<SchedulingTask> tasks = new ArrayList<>();

        tasks.add(null);
        tasks.add(
                task(
                        KitchenResourceType.GRILL,
                        8
                )
        );

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(tasks);

        assertEquals(
                8L,
                result.get(KitchenResourceType.GRILL)
                        .workloadMinutes()
        );
    }

    @Test
    void zeroDurationTaskIsIgnored() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, 0)
                ));

        assertZero(
                result,
                KitchenResourceType.GRILL
        );
    }

    @Test
    void negativeDurationTaskIsIgnored() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(KitchenResourceType.GRILL, -5)
                ));

        assertZero(
                result,
                KitchenResourceType.GRILL
        );
    }

    @Test
    void largeCumulativeWorkloadUsesLongAggregation() {

        Map<KitchenResourceType, ResourceWorkload> result =
                service.calculateWorkload(List.of(
                        task(
                                KitchenResourceType.GRILL,
                                Integer.MAX_VALUE
                        ),
                        task(
                                KitchenResourceType.GRILL,
                                Integer.MAX_VALUE
                        )
                ));

        long expected =
                (long) Integer.MAX_VALUE
                        + Integer.MAX_VALUE;

        assertEquals(
                expected,
                result.get(KitchenResourceType.GRILL)
                        .workloadMinutes()
        );

        assertEquals(
                1.0,
                result.get(KitchenResourceType.GRILL)
                        .congestion(),
                DELTA
        );
    }

    @Test
    void invalidPlanningWindowFailsFast() {

        try {
            new ResourceWorkloadServiceImpl(0);

        } catch (IllegalArgumentException ex) {

            assertTrue(
                    ex.getMessage()
                            .contains("planningWindowMinutes")
            );

            return;
        }

        throw new AssertionError(
                "Expected invalid planning window to fail"
        );
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

    private void assertAllResourcesPresent(
            Map<KitchenResourceType, ResourceWorkload> result
    ) {
        assertEquals(
                KitchenResourceType.values().length,
                result.size()
        );

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            assertTrue(
                    result.containsKey(resource)
            );
        }
    }

    private void assertZero(
            Map<KitchenResourceType, ResourceWorkload> result,
            KitchenResourceType resource
    ) {
        ResourceWorkload workload =
                result.get(resource);

        assertEquals(
                0L,
                workload.workloadMinutes()
        );

        assertEquals(
                0.0,
                workload.congestion(),
                DELTA
        );

        assertEquals(
                0.0,
                workload.pressure(),
                DELTA
        );
    }
}