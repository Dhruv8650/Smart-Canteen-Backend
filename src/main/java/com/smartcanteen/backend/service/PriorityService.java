package com.smartcanteen.backend.service;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PriorityService {

    @Value("${app.scheduling.resource-congestion-weight:1.0}")
    private double resourceCongestionWeight = 1.0;

    @Value("${app.scheduling.bottleneck-bonus-weight:0.5}")
    private double bottleneckBonusWeight = 0.5;

    @Value("${app.scheduling.resource-adjustment-cap:1.5}")
    private double resourceAdjustmentCap = 1.5;

    public PriorityService() {
    }

    PriorityService(
            double resourceCongestionWeight,
            double bottleneckBonusWeight,
            double resourceAdjustmentCap
    ) {
        this.resourceCongestionWeight = resourceCongestionWeight;
        this.bottleneckBonusWeight = bottleneckBonusWeight;
        this.resourceAdjustmentCap = resourceAdjustmentCap;
    }

    public double calculatePriority(Order order, int queueLoad) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        LocalDateTime createdAt = order.getCreatedAt();
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        long waitingMinutes = createdAt == null
                ? 0
                : Math.max(
                0,
                Duration.between(createdAt, nowUtc).toMinutes()
        );

        // Prevent priority explosion from waiting time.
        long cappedWaiting = Math.min(waitingMinutes, 60);

        int totalPrepTime =
                order.getTotalPrepTime() == null
                        || order.getTotalPrepTime() <= 0
                        ? 1
                        : order.getTotalPrepTime();

        // Existing adaptive queue-load weights.
        double waitingWeight =
                queueLoad >= 10 ? 0.3 : 0.7;

        double prepWeight =
                queueLoad >= 10 ? 2.0 : 1.0;

        double prepFactor =
                prepWeight / totalPrepTime;

        // Preserve existing PREPARING boost.
        double statusBoost = 0.0;

        if (order.getStatus() == OrderStatus.PREPARING) {
            statusBoost = 0.5;
        }

        return prepFactor
                + (cappedWaiting * waitingWeight)
                + statusBoost;
    }

    /**
     * Calculates the existing priority and adds the bounded
     * resource-aware scheduling adjustment.
     */
    public double calculatePriority(
            Order order,
            int queueLoad,
            List<SchedulingTask> orderTasks,
            Map<KitchenResourceType, ResourceWorkload> resourceWorkloads,
            Optional<ResourceBottleneck> bottleneck
    ) {

        double basePriority =
                calculatePriority(order, queueLoad);

        double resourceAdjustment =
                calculateResourceAdjustment(
                        orderTasks,
                        resourceWorkloads,
                        bottleneck
                );

        return basePriority + resourceAdjustment;
    }

    private double calculateResourceAdjustment(
            List<SchedulingTask> orderTasks,
            Map<KitchenResourceType, ResourceWorkload> resourceWorkloads,
            Optional<ResourceBottleneck> bottleneck
    ) {

        if (orderTasks == null
                || orderTasks.isEmpty()
                || resourceWorkloads == null
                || resourceWorkloads.isEmpty()) {

            return 0.0;
        }

        long totalDuration = 0;

        double weightedPressure = 0.0;

        long bottleneckDuration = 0;

        KitchenResourceType bottleneckResource =
                bottleneck == null
                        ? null
                        : bottleneck
                        .map(ResourceBottleneck::resource)
                        .orElse(null);

        double bottleneckPressure =
                bottleneck == null
                        ? 0.0
                        : bottleneck
                        .map(ResourceBottleneck::pressure)
                        .orElse(0.0);

        bottleneckPressure =
                clamp01(bottleneckPressure);

        for (SchedulingTask task : orderTasks) {

            if (task == null
                    || task.requiredResource() == null
                    || task.durationMinutes() <= 0) {

                continue;
            }

            int duration =
                    task.durationMinutes();

            ResourceWorkload workload =
                    resourceWorkloads.get(
                            task.requiredResource()
                    );

            double pressure =
                    workload == null
                            ? 0.0
                            : clamp01(workload.pressure());

            totalDuration += duration;

            weightedPressure +=
                    duration * pressure;

            if (task.requiredResource()
                    == bottleneckResource) {

                bottleneckDuration += duration;
            }
        }

        if (totalDuration <= 0) {
            return 0.0;
        }

        /*
         * Duration-weighted resource pressure:
         *
         * Σ(duration × pressure)
         * -----------------------
         *       Σ(duration)
         */
        double orderResourcePressure =
                weightedPressure / totalDuration;

        /*
         * Portion of this order's preparation work
         * that uses the current bottleneck resource.
         */
        double bottleneckShare =
                bottleneckDuration
                        / (double) totalDuration;

        /*
         * Bounded additive adjustment.
         */
        double resourceAdjustment =
                (resourceCongestionWeight
                        * orderResourcePressure)
                        +
                        (bottleneckBonusWeight
                                * bottleneckPressure
                                * bottleneckShare);

        return Math.min(
                resourceAdjustmentCap,
                Math.max(0.0, resourceAdjustment)
        );
    }

    private double clamp01(double value) {

        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }
}