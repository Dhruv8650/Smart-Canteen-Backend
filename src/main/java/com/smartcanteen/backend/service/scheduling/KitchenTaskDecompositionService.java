package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.Order;

import java.util.List;

public interface KitchenTaskDecompositionService {

    List<SchedulingTask> decompose(Order order);

}
