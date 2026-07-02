package io.github.kd656.coveragex.core.report.logic;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportConfig;
import io.github.kd656.coveragex.core.report.ReportContext;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.ReportScope;
import io.github.kd656.coveragex.core.report.ReportRenderer;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.model.ReportingType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused on the new multi-input overload {@code report(config, List&lt;ReportInput&gt;)}.
 * Uses a fake {@link ReportRenderer} to inspect what the service passes to renderers so
 * the assertion is on the actual model wired through the factory + pipeline.
 */
class ReportingServiceMultiInputTest {

    @Test
    void twoInputsProduceScopedModelWithBothScopes() {
        CapturingView view = new CapturingView();
        ReportingService service = new ReportingService(Map.of(ReportingType.HTML, view));

        service.report(config(), List.of(
            input("dto", "com/example/dto/UserRecord"),
            input("service", "com/example/service/UserService")));

        ReportModel model = view.lastModel;
        assertThat(model).isNotNull();
        assertThat(model.isScoped()).isTrue();
        assertThat(model.getScopes())
            .extracting(ReportScope::scopeId)
            .containsExactly("dto", "service");
    }

    @Test
    void singleInputWithNonMainScopeIdIsStillTreatedAsScoped() {
        CapturingView view = new CapturingView();
        ReportingService service = new ReportingService(Map.of(ReportingType.HTML, view));

        service.report(config(), List.of(input("only", "com/example/only/A")));

        assertThat(view.lastModel).isNotNull();
        assertThat(view.lastModel.isScoped()).isTrue();
        assertThat(view.lastModel.getScopes())
            .singleElement()
            .extracting(ReportScope::scopeId)
            .isEqualTo("only");
    }

    @Test
    void factoryBuildsScopedModelExactlyOnceAndFeedsBothPipelineAndViews() {
        // Fake factory counts invocations of the two build overloads and returns a
        // real model so the downstream pipeline and view still work. Any duplicate
        // build call (e.g. once per input, or once for pipeline and once for views)
        // would show up here.
        CountingModelFactory factory = new CountingModelFactory();
        CapturingView view = new CapturingView();
        ReportingService service = new ReportingService(
            factory, new ReportPipeline(), Map.of(ReportingType.HTML, view));

        service.report(config(), List.of(
            input("dto", "com/example/dto/UserRecord"),
            input("service", "com/example/service/UserService"),
            input("api", "com/example/api/UserApi")));

        assertThat(factory.listBuildCount).isEqualTo(1);
        assertThat(factory.dataBuildCount).isZero();
        assertThat(view.lastModel).isSameAs(factory.lastReturned);
    }

    @Test
    void viewReceivesTheContextFromReportConfig() {
        CapturingView view = new CapturingView();
        ReportingService service = new ReportingService(Map.of(ReportingType.HTML, view));

        ReportConfig config = config();
        service.report(config, List.of(input("dto", "com/example/dto/UserRecord")));

        assertThat(view.lastContext).isSameAs(config.context());
    }

    @Test
    void emptyInputListIsRejected() {
        ReportingService service = new ReportingService();
        assertThatThrownBy(() -> service.report(config(), List.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReportConfig config() {
        return new ReportConfig(
            Set.of(ReportingType.HTML),
            false, false, false, false, false,
            0.0,
            new ReportContext(null, null));
    }

    private static ReportInput input(String scopeId, String classId) {
        ClassCoverage cc = new ClassCoverage(classId, new boolean[]{true},
            Map.of(), Map.of(), List.of(), null);
        return new ReportInput(scopeId, scopeId, null,
            new ExecutionData(Map.of(classId, cc)));
    }

    private static final class CapturingView implements ReportRenderer {
        ReportModel lastModel;
        ReportContext lastContext;

        @Override
        public void render(ReportModel model, ReportContext context) {
            this.lastModel = model;
            this.lastContext = context;
        }

        @Override
        public ReportingType type() {
            return ReportingType.HTML;
        }
    }

    private static final class CountingModelFactory implements ReportModelFactory {
        int dataBuildCount;
        int listBuildCount;
        ReportModel lastReturned;
        final ReportModelFactory delegate = new DefaultReportModelFactory();

        @Override
        public ReportModel build(ExecutionData data) {
            dataBuildCount++;
            lastReturned = delegate.build(data);
            return lastReturned;
        }

        @Override
        public ReportModel build(List<ReportInput> inputs) {
            listBuildCount++;
            lastReturned = delegate.build(inputs);
            return lastReturned;
        }
    }
}
