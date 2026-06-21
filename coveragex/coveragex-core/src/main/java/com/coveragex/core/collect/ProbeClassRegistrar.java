package com.coveragex.core.collect;

import com.coveragex.api.data.ProbeMetadata;

import java.util.List;

/**
 * Registers instrumented classes with the coverage engine.
 *
 * <p>Called once per class by the {@code ClassTransformer} during instrumentation.
 * Kept separate from {@link ProbeRecorder} so the transformer depends only on the
 * registration surface and not on hot-path recording or snapshot reads.</p>
 */
public interface ProbeClassRegistrar {

    /**
     * Registers a class with the specified number of probes and static probe metadata.
     *
     * <p>The {@code metadata} list must contain exactly one entry per probe, in
     * {@code probeId} order, so that {@code metadata.get(probeId)} always resolves
     * correctly.</p>
     *
     * @param classId    the internal class name
     * @param probeCount the number of probes in the class
     * @param metadata   static metadata for every probe (determined at instrumentation time)
     * @return the probe hit array for this class
     */
    boolean[] registerClass(String classId, int probeCount, List<ProbeMetadata> metadata);
}
