package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

import java.time.LocalDateTime;
import  java.util.*;

public record ResourceTimelineSnapshot(
        Map<KitchenResourceType, ResourceTimeline> timelinesByResource,
        Map<Long, LocalDateTime> orderReadyAt,
        KitchenResourceType bottleneckResource,
        LocalDateTime schedulingTime
) {
    public ResourceTimelineSnapshot {
        timelinesByResource =
                immutableCompleteTimelineMap(
                        timelinesByResource,
                        schedulingTime
                );

        orderReadyAt =
                immutableOrderReadyAtMap(orderReadyAt);
    }

    private static Map<KitchenResourceType, ResourceTimeline>
    immutableCompleteTimelineMap(
            Map<KitchenResourceType, ResourceTimeline> source,
            LocalDateTime schedulingTime
    ) {
        Map<KitchenResourceType, ResourceTimeline> result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource :
                KitchenResourceType.values()) {

            ResourceTimeline timeline =
                    source == null
                            ? null
                            : source.get(resource);

            if (timeline == null) {
                timeline =
                        new ResourceTimeline(
                                resource,
                                java.util.List.of(),
                                schedulingTime
                        );
            }

            result.put(resource, timeline);
        }

        return Collections.unmodifiableMap(result);
    }

    private static Map<Long, LocalDateTime>
    immutableOrderReadyAtMap(
            Map<Long, LocalDateTime> source
    ) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(
                new HashMap<>(source)
        );
    }

    public int totalScheduledTasks() {
        int count = 0;

        for (ResourceTimeline timeline :
                timelinesByResource.values()) {

            count = Math.addExact(
                    count,
                    timeline.tasks().size()
            );
        }

        return count;
    }
}
