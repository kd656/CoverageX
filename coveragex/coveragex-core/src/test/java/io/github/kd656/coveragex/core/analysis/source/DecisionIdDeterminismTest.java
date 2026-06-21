package io.github.kd656.coveragex.core.analysis.source;

import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.DecisionModel;
import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the {@code NEXT_DECISION_ID} staleness hazard.
 *
 * <p>Earlier {@link SourceCodeAnalyzer} held the decision-id counter in a JVM-global
 * {@code static} field. Two analyzer invocations in the same JVM (e.g. two tests
 * in the same Surefire fork) would produce different decision IDs for the same
 * source depending on which test ran first — a classic test flake.</p>
 *
 * <p>This test runs the analyzer twice on identical source and asserts the resulting
 * {@code DecisionModel.decisionId} sequences are equal. It fails loudly if the
 * counter is ever returned to JVM-global state.</p>
 */
class DecisionIdDeterminismTest {

    private static final String SOURCE = """
            class Fixture {
                int classify(int v) {
                    if (v > 0) return 1;
                    if (v < 0) return -1;
                    return 0;
                }
            }
            """;

    @Test
    void decisionIds_areIdenticalAcrossTwoAnalyzerRunsInSameJvm(@TempDir Path tempA, @TempDir Path tempB) throws IOException {
        List<Integer> idsRunOne = runAnalyzerAndCollectDecisionIds(tempA);
        List<Integer> idsRunTwo = runAnalyzerAndCollectDecisionIds(tempB);

        assertThat(idsRunOne)
            .as("decision IDs must restart at 1 for each analyzer run; otherwise the static-counter hazard has regressed")
            .containsExactly(1, 2);
        assertThat(idsRunTwo).isEqualTo(idsRunOne);
    }

    private List<Integer> runAnalyzerAndCollectDecisionIds(Path srcRoot) throws IOException {
        Files.writeString(srcRoot.resolve("Fixture.java"), SOURCE);
        SemanticIndex index = new SemanticIndex();
        SourceCodeAnalyzer analyzer = new SourceCodeAnalyzer(new JavaParser(), index);
        analyzer.scan(srcRoot);

        List<Integer> ids = new ArrayList<>();
        for (ClassModel cm : index.getClasses().values()) {
            for (MethodModel mm : cm.getMethods().values()) {
                for (DecisionModel d : mm.getDecisionsList()) {
                    ids.add(d.decisionId());
                }
            }
        }
        return ids;
    }
}
