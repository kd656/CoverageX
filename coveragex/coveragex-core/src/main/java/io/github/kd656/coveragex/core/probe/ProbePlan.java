package io.github.kd656.coveragex.core.probe;

import io.github.kd656.coveragex.api.data.ProbeMetadata;

import java.util.List;

public record ProbePlan(String classId, List<ProbeMetadata> metadata) {

    public ProbePlan {
        metadata = List.copyOf(metadata);
    }

    public int probeCount() {
        return metadata.size();
    }
}
