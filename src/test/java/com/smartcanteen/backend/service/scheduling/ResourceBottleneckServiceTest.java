package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.impl.ResourceBottleneckServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceBottleneckServiceTest {

    private static final double DELTA = 0.000001;

    private final ResourceBottleneckService service =
            new ResourceBottleneckServiceImpl();

    @Test
    void nullWorkloadMapReturnsEmpty() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void emptyWorkloadMapReturnsEmpty() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(Map.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void allResourcesWithZeroPressureReturnsEmpty() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                0,
                                0.0,
                                0.0
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                0,
                                0.0,
                                0.0
                        ),
                        workload(
                                KitchenResourceType.BEVERAGE,
                                0,
                                0.0,
                                0.0
                        ),
                        workload(
                                KitchenResourceType.PREPARATION,
                                0,
                                0.0,
                                0.0
                        )
                ));

        assertTrue(result.isEmpty());
    }

    @Test
    void oneResourceWithPositivePressureIsSelected() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                12,
                                0.4,
                                0.4
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.GRILL,
                result.get().resource()
        );
    }

    @Test
    void highestPressureResourceIsSelected() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                24,
                                0.8,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                12,
                                0.4,
                                0.4
                        ),
                        workload(
                                KitchenResourceType.BEVERAGE,
                                6,
                                0.2,
                                0.2
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.GRILL,
                result.get().resource()
        );
    }

    @Test
    void highestPressureWinsEvenWhenAnotherResourceHasHigherWorkload() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                40,
                                1.0,
                                0.50
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                25,
                                0.8,
                                0.80
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void equalPressureUsesHigherCongestion() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                30,
                                0.5,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                20,
                                0.7,
                                0.8
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void equalPressureAndCongestionUsesHigherWorkload() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                10,
                                0.8,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                20,
                                0.8,
                                0.8
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void equalPressureCongestionAndWorkloadUsesEnumOrdinal() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.FRYER,
                                24,
                                0.8,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.GRILL,
                                24,
                                0.8,
                                0.8
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.GRILL,
                result.get().resource()
        );
    }

    @Test
    void nullResourceWorkloadValueIsIgnored() {

        Map<KitchenResourceType, ResourceWorkload> workloads =
                new EnumMap<>(KitchenResourceType.class);

        workloads.put(
                KitchenResourceType.GRILL,
                null
        );

        workloads.put(
                KitchenResourceType.FRYER,
                workload(
                        KitchenResourceType.FRYER,
                        15,
                        0.5,
                        0.5
                )
        );

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(workloads);

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void workloadWithNullResourceIsIgnored() {

        Map<KitchenResourceType, ResourceWorkload> workloads =
                new EnumMap<>(KitchenResourceType.class);

        workloads.put(
                KitchenResourceType.GRILL,
                new ResourceWorkload(
                        null,
                        30,
                        1.0,
                        1.0
                )
        );

        workloads.put(
                KitchenResourceType.BEVERAGE,
                workload(
                        KitchenResourceType.BEVERAGE,
                        6,
                        0.2,
                        0.2
                )
        );

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(workloads);

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.BEVERAGE,
                result.get().resource()
        );
    }

    @Test
    void negativeWorkloadIsIgnored() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                -1,
                                1.0,
                                1.0
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                12,
                                0.4,
                                0.4
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void negativePressureIsTreatedAsZeroAndIgnored() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                20,
                                0.8,
                                -0.1
                        )
                ));

        assertTrue(result.isEmpty());
    }

    @Test
    void pressureAboveOneIsClampedForComparison() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                10,
                                0.1,
                                1.2
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                20,
                                0.9,
                                1.0
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void congestionBelowZeroIsClampedForComparison() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                40,
                                -0.5,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                20,
                                0.1,
                                0.8
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void congestionAboveOneIsClampedForComparison() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.GRILL,
                                10,
                                1.5,
                                0.8
                        ),
                        workload(
                                KitchenResourceType.FRYER,
                                50,
                                1.0,
                                0.8
                        )
                ));

        assertTrue(result.isPresent());

        assertEquals(
                KitchenResourceType.FRYER,
                result.get().resource()
        );
    }

    @Test
    void returnedResourceBottleneckPreservesSelectedWorkloadValues() {

        Optional<ResourceBottleneck> result =
                service.detectBottleneck(mapOf(
                        workload(
                                KitchenResourceType.PREPARATION,
                                18,
                                0.6,
                                0.6
                        )
                ));

        assertTrue(result.isPresent());

        ResourceBottleneck bottleneck =
                result.get();

        assertEquals(
                KitchenResourceType.PREPARATION,
                bottleneck.resource()
        );

        assertEquals(
                18L,
                bottleneck.workloadMinutes()
        );

        assertEquals(
                0.6,
                bottleneck.congestion(),
                DELTA
        );

        assertEquals(
                0.6,
                bottleneck.pressure(),
                DELTA
        );
    }

    @Test
    void inputResourceWorkloadObjectsAreNotModified() {

        ResourceWorkload workload =
                workload(
                        KitchenResourceType.GRILL,
                        10,
                        1.5,
                        1.2
                );

        service.detectBottleneck(
                mapOf(workload)
        );

        assertEquals(
                KitchenResourceType.GRILL,
                workload.resource()
        );

        assertEquals(
                10L,
                workload.workloadMinutes()
        );

        assertEquals(
                1.5,
                workload.congestion(),
                DELTA
        );

        assertEquals(
                1.2,
                workload.pressure(),
                DELTA
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

    private Map<KitchenResourceType, ResourceWorkload> mapOf(
            ResourceWorkload... workloads
    ) {
        Map<KitchenResourceType, ResourceWorkload> result =
                new EnumMap<>(KitchenResourceType.class);

        for (ResourceWorkload workload : workloads) {

            if (workload.resource() == null) {
                throw new IllegalArgumentException(
                        "Use an explicit map for null-resource test cases"
                );
            }

            result.put(
                    workload.resource(),
                    workload
            );
        }

        return result;
    }
}