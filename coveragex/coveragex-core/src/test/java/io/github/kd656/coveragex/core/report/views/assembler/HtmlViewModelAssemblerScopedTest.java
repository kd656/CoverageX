package io.github.kd656.coveragex.core.report.views.assembler;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportInput;
import io.github.kd656.coveragex.core.report.logic.DefaultReportModelFactory;
import io.github.kd656.coveragex.core.report.model.ReportModel;
import io.github.kd656.coveragex.core.report.views.html.HtmlModuleNode;
import io.github.kd656.coveragex.core.report.views.html.HtmlNavNode;
import io.github.kd656.coveragex.core.report.views.html.HtmlReportViewModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlViewModelAssemblerScopedTest {

    @Test
    void singleModuleModelProducesFlatNavAndNoModules() {
        ReportModel model = new DefaultReportModelFactory()
            .build(new ExecutionData(Map.of(
                "com/example/Foo", coverage("com/example/Foo"))));

        HtmlReportViewModel viewModel = new HtmlViewModelAssembler().assemble(model, null);

        assertThat(viewModel.hasModules()).isFalse();
        assertThat(viewModel.modules()).isEmpty();
        assertThat(viewModel.navTree()).isNotEmpty();
        assertThat(viewModel.navTree().getFirst().isFolder()).isTrue();
    }

    @Test
    void multiModuleModelProducesOneHtmlModuleNodePerScope() {
        ReportModel model = new DefaultReportModelFactory().build(List.of(
            input("dto",     "com/example/dto/UserRecord"),
            input("service", "com/example/service/UserService")));

        HtmlReportViewModel viewModel = new HtmlViewModelAssembler().assemble(model, null);

        assertThat(viewModel.hasModules()).isTrue();
        assertThat(viewModel.navTree()).isEmpty();
        assertThat(viewModel.modules())
            .extracting(HtmlModuleNode::scopeId)
            .containsExactly("dto", "service");
    }

    @Test
    void scopedModulesEmitPrefixedSectionIdsAndScopedPayloadPaths() {
        ReportModel model = new DefaultReportModelFactory().build(List.of(
            input("dto", "com/example/dto/UserRecord")));

        HtmlReportViewModel viewModel = new HtmlViewModelAssembler().assemble(model, null);

        HtmlModuleNode dto = viewModel.modules().getFirst();
        HtmlNavNode.File leaf = firstFile(dto.navTree());
        assertThat(leaf.sectionId()).isEqualTo("dto--com-example-dto-UserRecord");
        assertThat(leaf.payloadPath()).isEqualTo("classes/dto/com-example-dto-UserRecord.data.js");
    }

    @Test
    void twoModulesWithSameSimpleClassNameDoNotClashInDomIdOrPayloadPath() {
        // Both modules ship a Utils class; without the scopeId prefix, the section ids
        // and payload paths would collide. Scoped layout is what keeps them distinct.
        ReportModel model = new DefaultReportModelFactory().build(List.of(
            input("frontend", "com/example/frontend/Utils"),
            input("backend",  "com/example/backend/Utils")));

        HtmlReportViewModel viewModel = new HtmlViewModelAssembler().assemble(model, null);

        HtmlNavNode.File frontend = firstFile(viewModel.modules().getFirst().navTree());
        HtmlNavNode.File backend  = firstFile(viewModel.modules().get(1).navTree());

        assertThat(frontend.sectionId()).startsWith("frontend--");
        assertThat(backend.sectionId()).startsWith("backend--");
        assertThat(frontend.payloadPath()).startsWith("classes/frontend/");
        assertThat(backend.payloadPath()).startsWith("classes/backend/");
    }

    @Test
    void topBarUsesAggregateSummaryAcrossScopes() {
        ReportModel model = new DefaultReportModelFactory().build(List.of(
            input("dto",     "com/example/dto/UserRecord"),
            input("service", "com/example/service/UserService")));

        HtmlReportViewModel viewModel = new HtmlViewModelAssembler().assemble(model, null);

        // Both inputs contributed 1 executed / 1 total probe; aggregate is 2/2.
        assertThat(viewModel.topBar().executedProbes()).isEqualTo(2);
        assertThat(viewModel.topBar().totalProbes()).isEqualTo(2);
    }

    private static ReportInput input(String scopeId, String classId) {
        return new ReportInput(scopeId, scopeId, null,
            new ExecutionData(Map.of(classId, coverage(classId))));
    }

    private static ClassCoverage coverage(String classId) {
        return new ClassCoverage(classId, new boolean[]{true},
            Map.of(), Map.of(), List.of(), null);
    }

    private static HtmlNavNode.File firstFile(List<HtmlNavNode> tree) {
        for (HtmlNavNode node : tree) {
            HtmlNavNode.File found = firstFileIn(node);
            if (found != null) return found;
        }
        throw new AssertionError("no file node found in tree");
    }

    private static HtmlNavNode.File firstFileIn(HtmlNavNode node) {
        if (!node.isFolder()) return (HtmlNavNode.File) node;
        for (HtmlNavNode child : ((HtmlNavNode.Folder) node).children()) {
            HtmlNavNode.File found = firstFileIn(child);
            if (found != null) return found;
        }
        return null;
    }
}
