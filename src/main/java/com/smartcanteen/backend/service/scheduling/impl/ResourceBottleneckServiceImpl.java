package com.smartcanteen.backend.service.scheduling.impl;

import com.smartcanteen.backend.dto.scheduling.ResourceBottleneck;
import com.smartcanteen.backend.dto.scheduling.ResourceWorkload;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.service.scheduling.ResourceBottleneckService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ResourceBottleneckServiceImpl implements ResourceBottleneckService {
    @Override
    public Optional<ResourceBottleneck> detectBottleneck(
            Map<KitchenResourceType, ResourceWorkload> workloads
    ) {
        if (workloads == null || workloads.isEmpty()) {
            return Optional.empty();
        }

        Candidate bestCandidate = null;

        for (ResourceWorkload workload : workloads.values()) {
            Candidate candidate = toCandidate(workload);

            if (candidate == null) {
                continue;
            }

            if (bestCandidate == null
                    || candidate.isBetterThan(bestCandidate)) {
                bestCandidate = candidate;
            }
        }

        if (bestCandidate == null) {
            return Optional.empty();
        }

        ResourceWorkload workload = bestCandidate.workload();

        return Optional.of(
                new ResourceBottleneck(
                        workload.resource(),
                        workload.workloadMinutes(),
                        workload.congestion(),
                        workload.pressure()
                )
        );
    }

    private Candidate toCandidate(ResourceWorkload workload) {

        if (workload == null
                || workload.resource() == null
                || workload.workloadMinutes() < 0) {
            return null;
        }

        double effectivePressure =
                clamp(workload.pressure());

        if (effectivePressure <= 0.0) {
            return null;
        }

        double effectiveCongestion =
                clamp(workload.congestion());

        return new Candidate(
                workload,
                effectivePressure,
                effectiveCongestion
        );
    }

    private double clamp(double value) {
        return Math.max(
                0.0,
                Math.min(1.0, value)
        );
    }

    private record Candidate(
            ResourceWorkload workload,
            double effectivePressure,
            double effectiveCongestion
    ) {

        boolean isBetterThan(Candidate other) {

            // 1. Higher pressure wins.
            int pressureComparison =
                    Double.compare(
                            effectivePressure,
                            other.effectivePressure
                    );

            if (pressureComparison != 0) {
                return pressureComparison > 0;
            }

            // 2. Higher congestion wins.
            int congestionComparison =
                    Double.compare(
                            effectiveCongestion,
                            other.effectiveCongestion
                    );

            if (congestionComparison != 0) {
                return congestionComparison > 0;
            }

            // 3. Higher workload wins.
            int workloadComparison =
                    Long.compare(
                            workload.workloadMinutes(),
                            other.workload.workloadMinutes()
                    );

            if (workloadComparison != 0) {
                return workloadComparison > 0;
            }

            // 4. Lower enum ordinal wins.
            return workload.resource().ordinal()
                    < other.workload.resource().ordinal();
        }
    }
}
