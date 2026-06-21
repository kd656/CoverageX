package com.coveragex.core.report.views.assembler;

import com.coveragex.core.insights.Insight;
import com.coveragex.core.insights.Severity;
import com.coveragex.core.report.model.*;
import com.coveragex.core.report.pipeline.results.InsightsResult;
import com.coveragex.core.report.views.html.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class HtmlViewModelAssembler {

    public HtmlReportViewModel assemble(ReportModel model, java.nio.file.Path sourceDirectory) {
        SummaryMetrics summary = model.getSummaryMetrics();
        List<ClassMetrics> classes = model.getClassMetrics();

        List<Insight> allInsights = model.get(InsightsResult.class)
            .map(InsightsResult::insights)
            .orElse(Collections.emptyList());

        Map<String, List<Insight>> insightsByClass = allInsights.stream()
            .collect(Collectors.groupingBy(Insight::classId));
        Map<Severity, Long> countBySeverity = allInsights.stream()
            .collect(Collectors.groupingBy(Insight::severity, Collectors.counting()));

        HtmlSummary topBar = buildTopBar(summary, countBySeverity);
        List<HtmlNavNode> navTree = buildNavTree(classes, insightsByClass);

        return new HtmlReportViewModel(topBar, navTree);
    }

    private HtmlSummary buildTopBar(SummaryMetrics summary, Map<Severity, Long> bySeverity) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        int notCovered = summary.totalProbes() - summary.executedProbes();
        return new HtmlSummary(
            timestamp, summary.lineCoveragePercent(),
            summary.classCount(), summary.totalProbes(), summary.executedProbes(), notCovered,
            bySeverity.getOrDefault(Severity.CRITICAL, 0L),
            bySeverity.getOrDefault(Severity.WARNING,  0L),
            bySeverity.getOrDefault(Severity.INFO,     0L),
            bySeverity.getOrDefault(Severity.POSITIVE, 0L)
        );
    }

    /**
     * Builds the IDE-style package tree from the flat list of class metrics.
     *
     * Algorithm:
     * 1. Build a mutable TreeNode tree by splitting each classId on '/'.
     * 2. Convert TreeNode tree depth-first to HtmlNavNode instances.
     * 3. Sort: folders first, then files, each group alphabetically.
     * 4. expandedByDefault = path contains first class, OR total class count <= 15.
     */
    List<HtmlNavNode> buildNavTree(List<ClassMetrics> classes,
                                   Map<String, List<Insight>> insightsByClass) {
        if (classes.isEmpty()) return Collections.emptyList();

        boolean smallProject = classes.size() <= 15;
        String firstClassId = classes.getFirst().classId();

        // Step 1: Build mutable intermediate tree
        TreeNode root = new TreeNode("", null);
        for (ClassMetrics cm : classes) {
            String[] segments = cm.classId().split("/");
            TreeNode current = root;
            // Navigate / create folder nodes for all segments except the last
            for (int i = 0; i < segments.length - 1; i++) {
                String seg = segments[i];
                final TreeNode parent = current;
                current = current.children.computeIfAbsent(seg, s -> new TreeNode(s, parent));
            }
            // Last segment is the class leaf
            String leafSeg = segments[segments.length - 1];
            current.children.put(leafSeg, new TreeNode(leafSeg, current, cm));
        }

        // Step 2: Convert to HtmlNavNode list
        return convertChildren(root, insightsByClass, firstClassId, smallProject, "");
    }

    private List<HtmlNavNode> convertChildren(TreeNode node,
                                              Map<String, List<Insight>> insightsByClass,
                                              String firstClassId,
                                              boolean smallProject,
                                              String parentPath) {
        List<HtmlNavNode> folders = new ArrayList<>();
        List<HtmlNavNode> files   = new ArrayList<>();

        for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
            String segment = entry.getKey();
            TreeNode child = entry.getValue();
            String childPath = parentPath.isEmpty() ? segment : parentPath + "/" + segment;

            if (child.leaf != null) {
                // This is a class file node
                ClassMetrics cm = child.leaf;
                List<Insight> ci = insightsByClass.getOrDefault(cm.classId(), Collections.emptyList());
                boolean hasCrit = ci.stream().anyMatch(i -> i.severity() == Severity.CRITICAL);
                boolean hasWarn = ci.stream().anyMatch(i -> i.severity() == Severity.WARNING);
                boolean hasPos  = ci.stream().anyMatch(i -> i.severity() == Severity.POSITIVE);
                files.add(new HtmlNavNode.File(
                    sectionId(cm.classId()),
                    cm.simpleName(),
                    cm.lineCoveragePercent(),
                    hasCrit, hasWarn, hasPos
                ));
            } else {
                // This is a folder (package segment)
                List<HtmlNavNode> grandchildren = convertChildren(
                    child, insightsByClass, firstClassId, smallProject, childPath);

                // Compute aggregate coverage across all descendants
                double avgCoverage = computeAverageCoverage(child);

                // Bubble up severity flags
                boolean folderHasCrit = anyDescendantHas(child, insightsByClass, Severity.CRITICAL);
                boolean folderHasWarn = anyDescendantHas(child, insightsByClass, Severity.WARNING);

                // expandedByDefault: path includes first class OR small project
                boolean expanded = smallProject || isOnPathToClass(child, firstClassId);

                folders.add(new HtmlNavNode.Folder(
                    segment,
                    childPath,
                    avgCoverage,
                    folderHasCrit,
                    folderHasWarn,
                    grandchildren,
                    expanded
                ));
            }
        }

        // Sort each group alphabetically
        folders.sort(Comparator.comparing(n -> ((HtmlNavNode.Folder) n).label()));
        files.sort(Comparator.comparing(n -> ((HtmlNavNode.File) n).simpleName()));

        List<HtmlNavNode> result = new ArrayList<>(folders.size() + files.size());
        result.addAll(folders);
        result.addAll(files);
        return result;
    }

    private double computeAverageCoverage(TreeNode node) {
        List<ClassMetrics> leaves = collectLeaves(node);
        if (leaves.isEmpty()) return 0.0;
        return leaves.stream()
            .mapToDouble(ClassMetrics::lineCoveragePercent)
            .average()
            .orElse(0.0);
    }

    private List<ClassMetrics> collectLeaves(TreeNode node) {
        List<ClassMetrics> result = new ArrayList<>();
        if (node.leaf != null) {
            result.add(node.leaf);
        } else {
            for (TreeNode child : node.children.values()) {
                result.addAll(collectLeaves(child));
            }
        }
        return result;
    }

    private boolean anyDescendantHas(TreeNode node, Map<String, List<Insight>> insightsByClass,
                                     Severity severity) {
        if (node.leaf != null) {
            return insightsByClass.getOrDefault(node.leaf.classId(), Collections.emptyList())
                .stream().anyMatch(i -> i.severity() == severity);
        }
        return node.children.values().stream()
            .anyMatch(child -> anyDescendantHas(child, insightsByClass, severity));
    }

    private boolean isOnPathToClass(TreeNode node, String classId) {
        if (node.leaf != null) return node.leaf.classId().equals(classId);
        return node.children.values().stream()
            .anyMatch(child -> isOnPathToClass(child, classId));
    }

    public static String sectionId(String classId) { return classId.replace('/', '-'); }

    // -----------------------------------------------------------------------
    // Mutable intermediate tree node
    // -----------------------------------------------------------------------

    private static final class TreeNode {
        final String segment;
        final TreeNode parent;
        final ClassMetrics leaf; // non-null only for leaf (file) nodes
        // Using LinkedHashMap to preserve insertion order for predictable output
        final Map<String, TreeNode> children = new LinkedHashMap<>();

        TreeNode(String segment, TreeNode parent) {
            this(segment, parent, null);
        }

        TreeNode(String segment, TreeNode parent, ClassMetrics leaf) {
            this.segment = segment;
            this.parent  = parent;
            this.leaf    = leaf;
        }
    }
}
