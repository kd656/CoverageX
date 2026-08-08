package io.github.kd656.coveragex.core.report.views.assembler;

import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.insights.Severity;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.model.MethodMetrics;
import io.github.kd656.coveragex.core.report.views.html.HtmlNavNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlNavTreeBuilderTest {

    @Test
    void emptyClassListYieldsEmptyTree() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(), Map.of(), "", HtmlNavTreeBuilderTest::flatPayload);
        assertThat(tree).isEmpty();
    }

    @Test
    void flatLayoutHasNoScopePrefixAndUsesFlatPayloadPath() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(cm("com/example/Foo")),
            Map.of(),
            "",
            HtmlNavTreeBuilderTest::flatPayload);

        HtmlNavNode.File leaf = firstFile(tree);
        assertThat(leaf.sectionId()).isEqualTo("com-example-Foo");
        assertThat(leaf.payloadPath()).isEqualTo("classes/com-example-Foo.data.js");
    }

    @Test
    void scopedLayoutAppliesPrefixAndScopedPayloadPath() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(cm("com/example/Foo")),
            Map.of(),
            "dto--",
            sid -> "classes/dto/" + sid + ".data.js");

        HtmlNavNode.File leaf = firstFile(tree);
        assertThat(leaf.sectionId()).isEqualTo("dto--com-example-Foo");
        assertThat(leaf.payloadPath()).isEqualTo("classes/dto/com-example-Foo.data.js");
    }

    @Test
    void foldersComeBeforeFilesAndBothAreAlphabetical() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(
                cm("com/example/z/Zed"),
                cm("com/example/a/Alpha"),
                cm("com/Zebra"),      // file directly under com/
                cm("com/Alpha")       // file directly under com/
            ),
            Map.of(),
            "",
            HtmlNavTreeBuilderTest::flatPayload);

        HtmlNavNode.Folder root = (HtmlNavNode.Folder) tree.getFirst();
        // Under com/: folders (a, z) sorted alphabetically, then files (Alpha, Zebra) alphabetically
        HtmlNavNode.Folder example = (HtmlNavNode.Folder) root.children().stream()
            .filter(HtmlNavNode::isFolder)
            .findFirst()
            .orElseThrow();
        assertThat(example.children())
            .filteredOn(HtmlNavNode::isFolder)
            .extracting(n -> ((HtmlNavNode.Folder) n).label())
            .containsExactly("a", "z");
    }

    @Test
    void sectionIdReplacesSlashesWithDashes() {
        assertThat(HtmlNavTreeBuilder.sectionId("com/example/Foo")).isEqualTo("com-example-Foo");
        assertThat(HtmlNavTreeBuilder.sectionId("NoPackageClass")).isEqualTo("NoPackageClass");
    }

    @Test
    void nestedClassesAreGroupedUnderOuterClass() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(
                cm("example/dto/UserTypes"),
                cm("example/dto/UserTypes$UserRecord")
            ),
            Map.of(),
            "dto--",
            sid -> "classes/dto/" + sid + ".data.js");

        HtmlNavNode.Folder example = (HtmlNavNode.Folder) tree.getFirst();
        HtmlNavNode.Folder dto = (HtmlNavNode.Folder) example.children().getFirst();
        HtmlNavNode.ClassGroup userTypes = (HtmlNavNode.ClassGroup) dto.children().getFirst();

        assertThat(userTypes.label()).isEqualTo("UserTypes");
        assertThat(userTypes.sectionId()).isEqualTo("dto--example-dto-UserTypes");
        assertThat(userTypes.children()).hasSize(1);

        HtmlNavNode.File nested = (HtmlNavNode.File) userTypes.children().getFirst();
        assertThat(nested.simpleName()).isEqualTo("UserRecord");
        assertThat(nested.sectionId()).isEqualTo("dto--example-dto-UserTypes$UserRecord");
        assertThat(nested.payloadPath()).isEqualTo("classes/dto/example-dto-UserTypes$UserRecord.data.js");
    }

    @Test
    void classGroupsAndPackageFoldersIncludeNestedClassesInAggregatedState() {
        List<HtmlNavNode> tree = new HtmlNavTreeBuilder().build(
            List.of(
                cm("example/dto/UserTypes", 100.0),
                cm("example/dto/UserTypes$UserRecord", 0.0)
            ),
            Map.of("example/dto/UserTypes$UserRecord", List.of(critical("example/dto/UserTypes$UserRecord"))),
            "",
            HtmlNavTreeBuilderTest::flatPayload);

        HtmlNavNode.Folder example = (HtmlNavNode.Folder) tree.getFirst();
        HtmlNavNode.Folder dto = (HtmlNavNode.Folder) example.children().getFirst();
        HtmlNavNode.ClassGroup userTypes = (HtmlNavNode.ClassGroup) dto.children().getFirst();

        assertThat(dto.averageCoveragePercent()).isEqualTo(50.0);
        assertThat(dto.hasCriticalInsight()).isTrue();
        assertThat(userTypes.hasCriticalInsight()).isTrue();
    }

    private static ClassMetrics cm(String classId) {
        return cm(classId, 50.0);
    }

    private static ClassMetrics cm(String classId, double lineCoveragePercent) {
        int slash = classId.lastIndexOf('/');
        // Mirror DefaultReportModelFactory: simpleName uses '.' between nested classes.
        String simple = (slash >= 0 ? classId.substring(slash + 1) : classId).replace('$', '.');
        String pkg = slash >= 0 ? classId.substring(0, slash).replace('/', '.') : "";
        int probeCount = 100;
        int hitProbeCount = (int) Math.round(lineCoveragePercent);
        MethodMetrics methodMetrics = new MethodMetrics("<init>", true, false, 1, 1,
            hitProbeCount, probeCount, hitProbeCount, 0, 0, List.of());
        return new ClassMetrics(classId, null, simple, pkg, lineCoveragePercent, 50.0, 50.0,
            List.of(methodMetrics), List.of(), List.of(), List.of(), null);
    }

    private static Insight critical(String classId) {
        return new Insight(classId, null, -1, "TEST", Severity.CRITICAL, "message", "hint");
    }

    private static HtmlNavNode.File firstFile(List<HtmlNavNode> tree) {
        for (HtmlNavNode node : tree) {
            HtmlNavNode.File found = firstFileIn(node);
            if (found != null) return found;
        }
        throw new AssertionError("no file node found in tree");
    }

    private static HtmlNavNode.File firstFileIn(HtmlNavNode node) {
        if (node instanceof HtmlNavNode.File file) return file;
        List<HtmlNavNode> children = switch (node) {
            case HtmlNavNode.Folder folder -> folder.children();
            case HtmlNavNode.ClassGroup group -> group.children();
            default -> List.of();
        };
        for (HtmlNavNode child : children) {
            HtmlNavNode.File found = firstFileIn(child);
            if (found != null) return found;
        }
        return null;
    }

    private static String flatPayload(String rawSectionId) {
        return "classes/" + rawSectionId + ".data.js";
    }
}
