package io.github.kd656.coveragex.core.probe;

import io.github.kd656.coveragex.api.data.ProbeMetadata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProbePlanBuilderTest {

    @Test
    void buildsStaticProbeMetadataWithoutTransformingClass() throws IOException {
        byte[] classBytes = classBytes(Fixture.class);
        String classId = Fixture.class.getName().replace('.', '/');

        ProbePlan plan = new ProbePlanBuilder().build(classId, classBytes, null);

        assertThat(plan.classId()).isEqualTo(classId);
        assertThat(plan.probeCount()).isEqualTo(plan.metadata().size());
        assertThat(plan.metadata()).allSatisfy(metadata -> assertThat(metadata).isNotNull());
        assertThat(plan.metadata()).anySatisfy(metadata ->
                assertThat(metadata).isInstanceOf(ProbeMetadata.MethodProbe.class));
        assertThat(plan.metadata()).anySatisfy(metadata ->
                assertThat(metadata).isInstanceOf(ProbeMetadata.BranchProbe.class));
        assertThat(plan.metadata()).anySatisfy(metadata ->
                assertThat(metadata).isInstanceOf(ProbeMetadata.ReturnProbe.class));
    }

    @Test
    void keepsProbeIdsAlignedWithMetadataIndexes() throws IOException {
        ProbePlan plan = new ProbePlanBuilder().build(null, classBytes(Fixture.class), null);

        List<ProbeMetadata> metadata = plan.metadata();
        for (int i = 0; i < metadata.size(); i++) {
            assertThat(metadata.get(i).probeId()).isEqualTo(i);
        }
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        try (var input = type.getResourceAsStream(resource)) {
            assertThat(input).isNotNull();
            return input.readAllBytes();
        }
    }

    static class Fixture {
        int classify(int value) {
            if (value > 0) {
                return 1;
            }
            return -1;
        }
    }
}
