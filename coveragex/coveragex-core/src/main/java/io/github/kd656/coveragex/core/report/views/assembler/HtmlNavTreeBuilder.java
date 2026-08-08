package io.github.kd656.coveragex.core.report.views.assembler;

import io.github.kd656.coveragex.core.insights.Insight;
import io.github.kd656.coveragex.core.insights.Severity;
import io.github.kd656.coveragex.core.report.model.ClassMetrics;
import io.github.kd656.coveragex.core.report.views.html.HtmlNavNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the IDE-style package tree of {@link HtmlNavNode}s from a flat class list.
 *
 * <p>Extracted from {@code HtmlViewModelAssembler} so that scoped (multi-module)
 * reports can build one tree per module while single-module reports keep a single
 * flat tree. Behavior is identical to the pre-extraction assembler when the
 * {@code sectionIdPrefix} is empty.</p>
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Build a mutable {@code TreeNode} tree by splitting each classId on {@code /}.</li>
 *   <li>Convert the {@code TreeNode} tree depth-first to {@link HtmlNavNode} instances.</li>
 *   <li>Sort: folders first, then files, each group alphabetically.</li>
 *   <li>{@code expandedByDefault} = path contains first class, OR total class count ≤ 15.</li>
 * </ol>
 */
public final class HtmlNavTreeBuilder {

    /**
     * @param classes         flat list of class metrics
     * @param insightsByClass insight lookup keyed by classId
     * @param sectionIdPrefix prefix prepended to each file node's sectionId; empty
     *                        for single-module reports, {@code "<scopeId>--"} for
     *                        scoped reports to keep DOM ids unique across modules
     * @param payloadPathFn   maps a raw sectionId to its {@code .data.js} relative
     *                        path; single-module uses {@code classes/&lt;id&gt;.data.js},
     *                        scoped uses {@code classes/&lt;scopeId&gt;/&lt;id&gt;.data.js}
     */
    public List<HtmlNavNode> build(List<ClassMetrics> classes,
                                    Map<String, List<Insight>> insightsByClass,
                                    String sectionIdPrefix,
                                    PayloadPathFunction payloadPathFn) {
        if (classes.isEmpty()) return Collections.emptyList();

        boolean smallProject = classes.size() <= 15;
        String firstClassId = classes.getFirst().classId();

        TreeNode root = new TreeNode("", null, NodeKind.PACKAGE);
        for (ClassMetrics cm : classes) {
            String[] segments = cm.classId().split("/");
            TreeNode current = root;
            for (int i = 0; i < segments.length - 1; i++) {
                String seg = segments[i];
                final TreeNode parent = current;
                current = current.children.computeIfAbsent(seg, s -> new TreeNode(s, parent, NodeKind.PACKAGE));
            }
            String[] classSegments = segments[segments.length - 1].split("\\$");
            for (int i = 0; i < classSegments.length; i++) {
                String classSegment = classSegments[i];
                final TreeNode parent = current;
                current = current.children.computeIfAbsent(
                        classSegment, s -> new TreeNode(s, parent, NodeKind.CLASS));
                if (i == classSegments.length - 1) {
                    current.leaf = cm;
                }
            }
        }

        return convertChildren(root, insightsByClass, firstClassId, smallProject, "",
                sectionIdPrefix, payloadPathFn);
    }

    /** Maps a raw sectionId (before prefixing) to the payload file path. */
    @FunctionalInterface
    public interface PayloadPathFunction {
        String pathFor(String rawSectionId);
    }

    private List<HtmlNavNode> convertChildren(TreeNode node,
                                               Map<String, List<Insight>> insightsByClass,
                                               String firstClassId,
                                               boolean smallProject,
                                               String parentPath,
                                               String sectionIdPrefix,
                                               PayloadPathFunction payloadPathFn) {
        List<HtmlNavNode> folders = new ArrayList<>();
        List<HtmlNavNode> files = new ArrayList<>();

        for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
            String segment = entry.getKey();
            TreeNode child = entry.getValue();
            String childPath = parentPath.isEmpty() ? segment : parentPath + "/" + segment;

            if (child.kind == NodeKind.CLASS && child.leaf != null && !child.children.isEmpty()) {
                files.add(toClassGroup(child, insightsByClass, firstClassId, smallProject, childPath,
                        sectionIdPrefix, payloadPathFn));
            } else if (child.leaf != null) {
                ClassMetrics cm = child.leaf;
                List<Insight> ci = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());
                boolean hasCrit = anyOfSeverity(ci, Severity.CRITICAL);
                boolean hasWarn = anyOfSeverity(ci, Severity.WARNING);
                boolean hasPos = anyOfSeverity(ci, Severity.POSITIVE);
                String rawSectionId = sectionId(cm.classId());
                String prefixedSectionId = sectionIdPrefix + rawSectionId;
                String payloadPath = payloadPathFn.pathFor(rawSectionId);
                files.add(new HtmlNavNode.File(
                        prefixedSectionId,
                        payloadPath,
                        displayName(cm),
                        cm.lineCoveragePercent(),
                        hasCrit, hasWarn, hasPos));
            } else {
                List<HtmlNavNode> grandchildren = convertChildren(
                        child, insightsByClass, firstClassId, smallProject, childPath,
                        sectionIdPrefix, payloadPathFn);
                double avgCoverage = computeAverageCoverage(child);
                boolean folderHasCrit = anyDescendantHas(child, insightsByClass, Severity.CRITICAL);
                boolean folderHasWarn = anyDescendantHas(child, insightsByClass, Severity.WARNING);
                boolean expanded = smallProject || isOnPathToClass(child, firstClassId);
                folders.add(new HtmlNavNode.Folder(
                        segment,
                        childPath,
                        avgCoverage,
                        folderHasCrit,
                        folderHasWarn,
                        grandchildren,
                        expanded));
            }
        }

        folders.sort(Comparator.comparing(n -> ((HtmlNavNode.Folder) n).label()));
        files.sort(Comparator.comparing(this::nodeLabel));

        List<HtmlNavNode> result = new ArrayList<>(folders.size() + files.size());
        result.addAll(folders);
        result.addAll(files);
        return result;
    }

    private HtmlNavNode.ClassGroup toClassGroup(TreeNode child,
                                                Map<String, List<Insight>> insightsByClass,
                                                String firstClassId,
                                                boolean smallProject,
                                                String childPath,
                                                String sectionIdPrefix,
                                                PayloadPathFunction payloadPathFn) {
        ClassMetrics cm = child.leaf;
        String rawSectionId = sectionId(cm.classId());
        String prefixedSectionId = sectionIdPrefix + rawSectionId;
        String payloadPath = payloadPathFn.pathFor(rawSectionId);
        List<Insight> ci = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());
        boolean hasCrit = anyOfSeverity(ci, Severity.CRITICAL)
                || anyChildHas(child, insightsByClass, Severity.CRITICAL);
        boolean hasWarn = anyOfSeverity(ci, Severity.WARNING)
                || anyChildHas(child, insightsByClass, Severity.WARNING);
        boolean hasPos = anyOfSeverity(ci, Severity.POSITIVE)
                || anyChildHas(child, insightsByClass, Severity.POSITIVE);
        boolean expanded = smallProject || isOnPathToClass(child, firstClassId);
        List<HtmlNavNode> children = convertChildren(
                child, insightsByClass, firstClassId, smallProject, childPath,
                sectionIdPrefix, payloadPathFn);
        return new HtmlNavNode.ClassGroup(
                prefixedSectionId,
                payloadPath,
                child.segment,
                cm.lineCoveragePercent(),
                hasCrit,
                hasWarn,
                hasPos,
                children,
                expanded);
    }

    private static boolean anyOfSeverity(List<Insight> insights, Severity severity) {
        for (Insight i : insights) {
            if (i.severity() == severity) return true;
        }
        return false;
    }

    private double computeAverageCoverage(TreeNode node) {
        List<ClassMetrics> leaves = new ArrayList<>();
        collectLeaves(node, leaves);
        return ClassMetrics.aggregateProbeCoverage(leaves);
    }

    private void collectLeaves(TreeNode node, List<ClassMetrics> into) {
        if (node.leaf != null) {
            into.add(node.leaf);
        }
        for (TreeNode child : node.children.values()) {
            collectLeaves(child, into);
        }
    }

    private boolean anyDescendantHas(TreeNode node,
                                      Map<String, List<Insight>> insightsByClass,
                                      Severity severity) {
        if (node.leaf != null) {
            if (anyOfSeverity(
                    insightsByClass.getOrDefault(node.leaf.classId(), Collections.emptyList()),
                    severity)) {
                return true;
            }
        }
        for (TreeNode child : node.children.values()) {
            if (anyDescendantHas(child, insightsByClass, severity)) return true;
        }
        return false;
    }

    private boolean anyChildHas(TreeNode node,
                                Map<String, List<Insight>> insightsByClass,
                                Severity severity) {
        for (TreeNode child : node.children.values()) {
            if (anyDescendantHas(child, insightsByClass, severity)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnPathToClass(TreeNode node, String classId) {
        if (node.leaf != null && node.leaf.classId().equals(classId)) return true;
        for (TreeNode child : node.children.values()) {
            if (isOnPathToClass(child, classId)) return true;
        }
        return false;
    }

    private String displayName(ClassMetrics cm) {
        String simpleName = cm.simpleName();
        int dot = simpleName.lastIndexOf('.');
        return dot >= 0 ? simpleName.substring(dot + 1) : simpleName;
    }

    private String nodeLabel(HtmlNavNode node) {
        if (node instanceof HtmlNavNode.File file) {
            return file.simpleName();
        }
        if (node instanceof HtmlNavNode.ClassGroup group) {
            return group.label();
        }
        if (node instanceof HtmlNavNode.Folder folder) {
            return folder.label();
        }
        return "";
    }

    /** Same section-id derivation used by both flat and scoped reports. */
    public static String sectionId(String classId) {
        return classId.replace('/', '-');
    }

    /** Mutable intermediate node used while building the tree. */
    private static final class TreeNode {
        final String segment;
        final TreeNode parent;
        final NodeKind kind;
        ClassMetrics leaf;
        final Map<String, TreeNode> children = new LinkedHashMap<>();

        TreeNode(String segment, TreeNode parent, NodeKind kind) {
            this.segment = segment;
            this.parent = parent;
            this.kind = kind;
        }
    }

    private enum NodeKind {
        PACKAGE,
        CLASS
    }
}
