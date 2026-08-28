package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.KitchenResourceType;

import java.util.List;
import java.util.Map;

public interface ResourceWorkloadService {
    Map<KitchenResourceType, ResourceWorkload> calculateWorkload(
            List<SchedulingTask> tasks);
}
