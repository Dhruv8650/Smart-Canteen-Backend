package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

import java.util.*;

public record ResourceScheduleSnapshot(
        Map<KitchenResourceType, List<ScheduledResourceTask>> tasksByResource,
                                       KitchenResourceType bottleneckResource,



                                       int totalTasks)
{
    public ResourceScheduleSnapshot {
        tasksByResource = immutableCompleteResourceMap(tasksByResource);
        totalTasks = countTasks(tasksByResource);
    }

    private static Map<KitchenResourceType, List<ScheduledResourceTask>>
    immutableCompleteResourceMap(
            Map<KitchenResourceType, List<ScheduledResourceTask>> source
    ) {
        Map<KitchenResourceType, List<ScheduledResourceTask>> result =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource : KitchenResourceType.values()) {
            List<ScheduledResourceTask> tasks =
                    source == null
                            ? List.of()
                            : source.get(resource);

            if (tasks == null) {
                tasks = List.of();
            }

            result.put(
                    resource,
                    Collections.unmodifiableList(new ArrayList<>(tasks))
            );
        }

        return Collections.unmodifiableMap(result);
    }

    private static int countTasks(
            Map<KitchenResourceType, List<ScheduledResourceTask>> tasksByResource
    ) {
        int count = 0;

        for (List<ScheduledResourceTask> tasks : tasksByResource.values()) {
            count += tasks.size();
        }

        return count;
    }
}
