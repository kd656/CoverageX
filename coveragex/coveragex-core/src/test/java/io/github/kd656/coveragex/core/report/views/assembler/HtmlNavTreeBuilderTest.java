package io.github.kd656.coveragex.core.report.views.assembler;

import io.github.kd656.coveragex.core.report.model.ClassMetrics;
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

    private static ClassMetrics cm(String classId) {
        int slash = classId.lastIndexOf('/');
        String simple = slash >= 0 ? classId.substring(slash + 1) : classId;
        String pkg = slash >= 0 ? classId.substring(0, slash).replace('/', '.') : "";
        return new ClassMetrics(classId, simple, pkg, 50.0, 50.0, 50.0,
            List.of(), List.of(), List.of(), List.of(), null);
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

    private static String flatPayload(String rawSectionId) {
        return "classes/" + rawSectionId + ".data.js";
    }
}
