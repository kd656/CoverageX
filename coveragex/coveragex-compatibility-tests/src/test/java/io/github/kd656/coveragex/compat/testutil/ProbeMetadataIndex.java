package io.github.kd656.coveragex.compat.testutil;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.core.probe.ProbePlan;
import io.github.kd656.coveragex.core.probe.ProbePlanBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a probe ID back to its {@link ProbeMetadata}. Built by re-running
 * {@link ProbePlanBuilder} on the same raw class bytes the agent test instruments —
 * the planner's emission order matches the injector's, so probe IDs align.
 *
 * <p>Used by {@link io.github.kd656.coveragex.compat.contract.HitsContract} to resolve hit
 * probe IDs back to their semantic types (MethodProbe / BranchProbe / ReturnProbe /
 * ThrowProbe) without coupling contracts to JDK-shape-sensitive raw IDs.</p>
 */
public final class ProbeMetadataIndex {

    private final Map<Integer, ProbeMetadata> byId;

    private ProbeMetadataIndex(Map<Integer, ProbeMetadata> byId) {
        this.byId = byId;
    }

    public static ProbeMetadataIndex from(byte[] rawClass) {
        ProbePlan plan = new ProbePlanBuilder().build("__index", rawClass, null);
        Map<Integer, ProbeMetadata> map = new HashMap<>();
        for (ProbeMetadata m : plan.metadata()) {
            map.put(m.probeId(), m);
        }
        return new ProbeMetadataIndex(map);
    }

    public ProbeMetadata metadataOf(int probeId) {
        return byId.get(probeId);
    }
}
