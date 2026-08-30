package com.smartcanteen.backend.service.scheduling.impl;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ScheduledResourceTask;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.service.scheduling.ResourceAwareSchedulingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ResourceAwareSchedulingServiceImpl
        implements ResourceAwareSchedulingService {

    @Override
    public ResourceScheduleSnapshot buildDispatchSnapshot(
            List<Order> orders,
            Map<Long, List<SchedulingTask>> tasksByOrderId,
            Optional<ResourceBottleneck> bottleneck
    ) {
        Map<KitchenResourceType, List<ScheduledResourceTask>> queues =
                initializeQueues();

        Map<KitchenResourceType, Integer> nextSequenceByResource =
                initializeSequences();

        if (orders != null && tasksByOrderId != null) {
            for (Order order : orders) {
                if (order == null) {
                    continue;
                }

                addOrderTasksToResourceQueues(
                        queues,
                        nextSequenceByResource,
                        tasksByOrderId.get(order.getId())
                );
            }
        }

        return new ResourceScheduleSnapshot(
                queues,
                resolveBottleneckResource(bottleneck),
                countTasks(queues)
        );
    }

    private Map<KitchenResourceType, List<ScheduledResourceTask>>
    initializeQueues() {
        Map<KitchenResourceType, List<ScheduledResourceTask>> queues =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource : KitchenResourceType.values()) {
            queues.put(resource, new ArrayList<>());
        }

        return queues;
    }

    private Map<KitchenResourceType, Integer> initializeSequences() {
        Map<KitchenResourceType, Integer> sequences =
                new EnumMap<>(KitchenResourceType.class);

        for (KitchenResourceType resource : KitchenResourceType.values()) {
            sequences.put(resource, 0);
        }

        return sequences;
    }

    private void addOrderTasksToResourceQueues(
            Map<KitchenResourceType, List<ScheduledResourceTask>> queues,
            Map<KitchenResourceType, Integer> nextSequenceByResource,
            List<SchedulingTask> tasks
    ) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (SchedulingTask task : tasks) {
            if (!isValidTask(task)) {
                continue;
            }

            KitchenResourceType resource = task.requiredResource();

            int sequence =
                    nextSequenceByResource.get(resource) + 1;

            nextSequenceByResource.put(
                    resource,
                    sequence
            );

            queues.get(resource)
                    .add(
                            new ScheduledResourceTask(
                                    task.orderId(),
                                    task.orderItemId(),
                                    task.foodItemId(),
                                    resource,
                                    task.durationMinutes(),
                                    sequence
                            )
                    );
        }
    }

    private boolean isValidTask(SchedulingTask task) {
        return task != null
                && task.requiredResource() != null
                && task.durationMinutes() > 0;
    }

    private KitchenResourceType resolveBottleneckResource(
            Optional<ResourceBottleneck> bottleneck
    ) {
        if (bottleneck == null || bottleneck.isEmpty()) {
            return null;
        }

        return bottleneck
                .map(ResourceBottleneck::resource)
                .orElse(null);
    }

    private int countTasks(
            Map<KitchenResourceType, List<ScheduledResourceTask>> queues
    ) {
        int count = 0;

        for (List<ScheduledResourceTask> tasks : queues.values()) {
            count += tasks.size();
        }

        return count;
    }
}