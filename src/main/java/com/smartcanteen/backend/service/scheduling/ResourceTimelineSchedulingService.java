package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceScheduleSnapshot;
import com.smartcanteen.backend.dto.scheduling.ResourceTimelineSnapshot;

import java.time.LocalDateTime;

public interface ResourceTimelineSchedulingService {
    ResourceTimelineSnapshot buildTimeline(
            ResourceScheduleSnapshot snapshot,
            LocalDateTime schedulingTime
    );
}
