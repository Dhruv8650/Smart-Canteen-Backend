package com.smartcanteen.backend.service.scheduling.impl;

import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ResourceTimeline;
import com.smartcanteen.backend.dto.scheduling.ResourceTimelineSnapshot;
import com.smartcanteen.backend.dto.scheduling.ScheduledResourceTask;
import com.smartcanteen.backend.dto.scheduling.ScheduledTaskTimeline;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.ResourceTimelineSchedulingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ResourceTimelineSchedulingServiceImpl
        implements ResourceTimelineSchedulingService {

    @Override
    public ResourceTimelineSnapshot buildTimeline(
            ResourceScheduleSnapshot snapshot,
            LocalDateTime schedulingTime
    ) {
        Objects.requireNonNull(
                schedulingTime,
                "schedulingTime must not be null"
        );

        Map<KitchenResourceType, LocalDateTime>
                resourceAvailableAt =
                initializeResourceAvailability(
                        schedulingTime
                );

        Map<KitchenResourceType, List<ScheduledTaskTimeline>>
                timelineTasksByResource =
                initializeTimelineTaskLists();

        Map<Long, LocalDateTime> orderReadyAt =
                new HashMap<>();

        if (snapshot != null) {
            scheduleSnapshotTasks(
                    snapshot,
                    resourceAvailableAt,
                    timelineTasksByResource,
                    orderReadyAt
            );
        }

        Map<KitchenResourceType, ResourceTimeline>
                timelinesByResource =
                buildResourceTimelines(
                        resourceAvailableAt,
                        timelineTasksByResource
                );

        return new ResourceTimelineSnapshot(
                timelinesByResource,
                orderReadyAt,
                snapshot == null
                        ? null
                        : snapshot.bottleneckResource(),
                schedulingTime
        );
    }

    private Map<KitchenResourceType, LocalDateTime>
    initializeResourceAvailability(
            LocalDateTime schedulingTime
    ) {
        Map<KitchenResourceType, LocalDateTime> result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            result.put(
                    resource,
                    schedulingTime
            );
        }

        return result;
    }

    private Map<KitchenResourceType, List<ScheduledTaskTimeline>>
    initializeTimelineTaskLists() {

        Map<KitchenResourceType, List<ScheduledTaskTimeline>>
                result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            result.put(
                    resource,
                    new ArrayList<>()
            );
        }

        return result;
    }

    private void scheduleSnapshotTasks(
            ResourceScheduleSnapshot snapshot,
            Map<KitchenResourceType, LocalDateTime>
                    resourceAvailableAt,
            Map<KitchenResourceType, List<ScheduledTaskTimeline>>
                    timelineTasksByResource,
            Map<Long, LocalDateTime> orderReadyAt
    ) {

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            LocalDateTime currentAvailableAt =
                    resourceAvailableAt.get(resource);

            List<ScheduledResourceTask> tasks =
                    snapshot.tasksByResource() == null
                            ? List.of()
                            : snapshot.tasksByResource()
                            .get(resource);

            if (tasks == null) {
                tasks = List.of();
            }

            for (ScheduledResourceTask task : tasks) {

                if (!isValidTask(task)) {
                    continue;
                }

                ScheduledTaskTimeline timelineTask =
                        createTimelineTask(
                                task,
                                currentAvailableAt
                        );

                timelineTasksByResource
                        .get(resource)
                        .add(timelineTask);

                currentAvailableAt =
                        timelineTask.endTime();

                resourceAvailableAt.put(
                        resource,
                        currentAvailableAt
                );

                updateOrderReadyAt(
                        orderReadyAt,
                        timelineTask
                );
            }
        }
    }

    private boolean isValidTask(
            ScheduledResourceTask task
    ) {
        return task != null
                && task.resource() != null
                && task.durationMinutes() > 0;
    }

    private ScheduledTaskTimeline createTimelineTask(
            ScheduledResourceTask task,
            LocalDateTime startTime
    ) {

        LocalDateTime endTime =
                startTime.plusMinutes(
                        task.durationMinutes()
                );

        return new ScheduledTaskTimeline(
                task.orderId(),
                task.orderItemId(),
                task.foodItemId(),
                task.resource(),
                task.durationMinutes(),
                task.sequence(),
                startTime,
                endTime
        );
    }

    private void updateOrderReadyAt(
            Map<Long, LocalDateTime> orderReadyAt,
            ScheduledTaskTimeline task
    ) {

        if (task.orderId() == null) {
            return;
        }

        LocalDateTime previousReadyAt =
                orderReadyAt.get(task.orderId());

        if (previousReadyAt == null
                || task.endTime().isAfter(previousReadyAt)) {

            orderReadyAt.put(
                    task.orderId(),
                    task.endTime()
            );
        }
    }

    private Map<KitchenResourceType, ResourceTimeline>
    buildResourceTimelines(
            Map<KitchenResourceType, LocalDateTime>
                    resourceAvailableAt,
            Map<KitchenResourceType, List<ScheduledTaskTimeline>>
                    timelineTasksByResource
    ) {

        Map<KitchenResourceType, ResourceTimeline>
                result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            List<ScheduledTaskTimeline> tasks =
                    timelineTasksByResource.get(resource);

            result.put(
                    resource,
                    new ResourceTimeline(
                            resource,
                            tasks,
                            resourceAvailableAt.get(resource)
                    )
            );
        }

        return result;
    }
}