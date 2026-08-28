package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.entity.KitchenResourceType;

import java.util.Map;
import java.util.Optional;

public interface ResourceBottleneckService {
    Optional<ResourceBottleneck> detectBottleneck(
            Map<KitchenResourceType, ResourceWorkload> workloads
    );
}
