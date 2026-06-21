package com.coveragex.core.instrument;

import com.coveragex.core.collect.CommonCoverageDataCollector;
import com.coveragex.core.instrument.DefaultProbeInjector;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.io.CoverageDataWriter;
import com.coveragex.core.probe.ProbePlan;
import com.coveragex.core.probe.ProbePlanBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProbePlanParityTest {

    @Test
    void defaultInjectorAndStaticPlannerProduceIdenticalProbeMetadata() throws IOException {
        byte[] classBytes = classBytes(Fixture.class);
        String classId = Fixture.class.getName().replace('.', '/');

        ProbePlan staticPlan = new ProbePlanBuilder().build(classId, classBytes, null);

        CommonCoverageDataCollector collector = new CommonCoverageDataCollector(new NoopCoverageDataWriter());
        new DefaultProbeInjector(collector).injectProbes(classId, classBytes);

        ClassCoverage runtimeCoverage = collector.snapshot().classCoverage(classId);
        assertThat(runtimeCoverage).isNotNull();
        assertThat(runtimeCoverage.probeMetadata()).isEqualTo(staticPlan.metadata());
        assertThat(runtimeCoverage.probeHits()).hasSize(staticPlan.probeCount());
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var input = type.getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            return input.readAllBytes();
        }
    }

    private static final class NoopCoverageDataWriter implements CoverageDataWriter<ExecutionData> {
        @Override
        public void write(Path outputPath, ExecutionData executionData) {
        }
    }

    static class Fixture {
        String label(int value) {
            if (value > 10) {
                return "high";
            }
            if (value == 10) {
                return "ten";
            }
            return "low";
        }
    }
}
