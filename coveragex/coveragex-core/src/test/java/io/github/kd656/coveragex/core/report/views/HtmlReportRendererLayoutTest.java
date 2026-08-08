package io.github.kd656.coveragex.core.report.views;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportContext;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.logic.DefaultReportModelFactory;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Locks in the on-disk output shape for both layouts.
 *
 * <p>Single-module reports must keep the flat {@code classes/&lt;sectionId&gt;.data.js}
 * layout untouched — that's the compatibility promise from §6.6 of the design.
 * Scoped reports put each scope's payloads under {@code classes/&lt;scopeId&gt;/...}.</p>
 */
class HtmlReportRendererLayoutTest {

    @Test
    void singleModuleReportUsesFlatClassesLayout(@TempDir Path tmp) {
        ReportModel model = new DefaultReportModelFactory()
            .build(new ExecutionData(Map.of(
                "com/example/Foo", coverage("com/example/Foo"))));

        new HtmlReportRenderer().render(model, new ReportContext(tmp, null));

        assertThat(Files.exists(tmp.resolve("index.html"))).isTrue();
        assertThat(Files.exists(tmp.resolve("classes/com-example-Foo.data.js"))).isTrue();
        // No scope subdirectory in single-module mode.
        assertThat(Files.exists(tmp.resolve("classes/main"))).isFalse();
    }

    @Test
    void scopedReportPutsEachModulePayloadUnderScopeSubdirectory(@TempDir Path tmp) {
        ReportModel model = new DefaultReportModelFactory().build(List.of(
            input("dto",     "com/example/dto/UserRecord"),
            input("service", "com/example/service/UserService")));

        new HtmlReportRenderer().render(model, new ReportContext(tmp, null));

        assertThat(Files.exists(tmp.resolve("index.html"))).isTrue();
        assertThat(Files.exists(tmp.resolve("classes/dto/com-example-dto-UserRecord.data.js"))).isTrue();
        assertThat(Files.exists(tmp.resolve("classes/service/com-example-service-UserService.data.js"))).isTrue();
        // Flat paths must not exist in scoped mode — that would mean payloads landed twice.
        assertThat(Files.exists(tmp.resolve("classes/com-example-dto-UserRecord.data.js"))).isFalse();
        assertThat(Files.exists(tmp.resolve("classes/com-example-service-UserService.data.js"))).isFalse();
    }

    private static ReportInput input(String scopeId, String classId) {
        return new ReportInput(scopeId, scopeId, null,
            new ExecutionData(Map.of(classId, coverage(classId))), Map.of());
    }

    private static ClassCoverage coverage(String classId) {
        return new ClassCoverage(classId, new boolean[]{true},
            Map.of(), Map.of(), List.of(), null);
    }
}
