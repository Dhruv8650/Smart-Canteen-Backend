package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.Order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ResourceAwareSchedulingService {
    ResourceScheduleSnapshot buildDispatchSnapshot(
            List<Order> orders,
            Map<Long, List<SchedulingTask>> tasksByOrderId,
            Optional<ResourceBottleneck> bottleneck
    );
}
