package com.smartcanteen.backend.service.scheduling.impl;

import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.ResourceWorkloadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ResourceWorkloadServiceImpl implements ResourceWorkloadService {

    private final int planningWindowMinutes;

    public ResourceWorkloadServiceImpl(
            @Value("${app.scheduling.planning-window-minutes:30}")
            int planningWindowMinutes
    ) {
        if (planningWindowMinutes <= 0) {
            throw new IllegalArgumentException(
                    "planningWindowMinutes must be greater than zero"
            );
        }

        this.planningWindowMinutes = planningWindowMinutes;
    }

    @Override
    public Map<KitchenResourceType, ResourceWorkload> calculateWorkload(
            List<SchedulingTask> tasks
    ) {
        Map<KitchenResourceType, Long> workloadByResource =
                initializeWorkloadMap();

        if (tasks != null) {
            for (SchedulingTask task : tasks) {
                addTaskWorkload(workloadByResource, task);
            }
        }

        return buildResult(workloadByResource);
    }

    private Map<KitchenResourceType, Long> initializeWorkloadMap() {
        Map<KitchenResourceType, Long> workloadByResource =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource : KitchenResourceType.values()) {
            workloadByResource.put(resource, 0L);
        }

        return workloadByResource;
    }

    private void addTaskWorkload(
            Map<KitchenResourceType, Long> workloadByResource,
            SchedulingTask task
    ) {
        if (task == null || task.requiredResource() == null) {
            return;
        }

        int durationMinutes = task.durationMinutes();

        if (durationMinutes <= 0) {
            return;
        }

        workloadByResource.merge(
                task.requiredResource(),
                (long) durationMinutes,
                Long::sum
        );
    }

    private Map<KitchenResourceType, ResourceWorkload> buildResult(
            Map<KitchenResourceType, Long> workloadByResource
    ) {
        Map<KitchenResourceType, ResourceWorkload> result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource : KitchenResourceType.values()) {

            long workloadMinutes =
                    workloadByResource.getOrDefault(resource, 0L);

            double congestion =
                    calculateCongestion(workloadMinutes);

            double pressure = congestion;

            result.put(
                    resource,
                    new ResourceWorkload(
                            resource,
                            workloadMinutes,
                            congestion,
                            pressure
                    )
            );
        }

        return result;
    }

    private double calculateCongestion(long workloadMinutes) {

        double rawCongestion =
                workloadMinutes / (double) planningWindowMinutes;

        return Math.max(
                0.0,
                Math.min(1.0, rawCongestion)
        );
    }
}
