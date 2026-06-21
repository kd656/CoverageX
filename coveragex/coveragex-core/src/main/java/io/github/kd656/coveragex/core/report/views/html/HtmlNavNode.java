package io.github.kd656.coveragex.core.report.views.html;

import java.util.List;

/**
 * A node in the IDE-style tree navigation sidebar.
 * Folders represent packages; files represent individual classes.
 */
public sealed interface HtmlNavNode permits HtmlNavNode.Folder, HtmlNavNode.File {

    default boolean isFolder() { return false; }

    record Folder(
        String label,
        String path,
        double averageCoveragePercent,
        boolean hasCriticalInsight,
        boolean hasWarningInsight,
        List<HtmlNavNode> children,
        boolean expandedByDefault
    ) implements HtmlNavNode {
        @Override
        public boolean isFolder() { return true; }
    }

    record File(
        String sectionId,
        String simpleName,
        double coveragePercent,
        boolean hasCriticalInsight,
        boolean hasWarningInsight,
        boolean hasPositiveInsight
    ) implements HtmlNavNode {}
}
