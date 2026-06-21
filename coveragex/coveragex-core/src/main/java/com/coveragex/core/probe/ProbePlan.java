package com.coveragex.core.probe;

import com.coveragex.api.data.ProbeMetadata;

import java.util.List;

public record ProbePlan(String classId, List<ProbeMetadata> metadata) {

    public ProbePlan {
        metadata = List.copyOf(metadata);
    }

    public int probeCount() {
        return metadata.size();
    }
}
