package com.smartcanteen.backend.dto.scheduling;

import com.smartcanteen.backend.entity.KitchenResourceType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ResourceTimeline(
        KitchenResourceType resource,
        List<ScheduledTaskTimeline> tasks,
        LocalDateTime availableAt
) {
    public ResourceTimeline {
        tasks = immutableTaskList(tasks);
    }

    private static List<ScheduledTaskTimeline> immutableTaskList(
            List<ScheduledTaskTimeline> source
    ) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(source)
        );
    }

    public int totalDurationMinutes() {
        int total = 0;

        for (ScheduledTaskTimeline task : tasks) {
            if (task == null || task.durationMinutes() <= 0) {
                continue;
            }

            total = Math.addExact(
                    total,
                    task.durationMinutes()
            );
        }

        return total;
    }

}
