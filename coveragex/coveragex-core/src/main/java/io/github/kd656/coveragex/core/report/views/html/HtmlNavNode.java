package io.github.kd656.coveragex.core.report.views.html;

import java.util.List;

/**
 * A node in the IDE-style tree navigation sidebar.
 * Folders represent packages; files represent individual classes.
 *
 * <p><b>Extension note:</b> fields may grow as new HTML features ship. Prefer
 * accessor calls ({@code node.sectionId()}) over pattern matching
 * ({@code case File(var sectionId, ...) -> ...}) so future field additions do
 * not break call sites.</p>
 */
public sealed interface HtmlNavNode permits HtmlNavNode.Folder, HtmlNavNode.File, HtmlNavNode.ClassGroup {

    default boolean isFolder() { return false; }
    default boolean isClassGroup() { return false; }

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
        String payloadPath,
        String simpleName,
        double coveragePercent,
        boolean hasCriticalInsight,
        boolean hasWarningInsight,
        boolean hasPositiveInsight
    ) implements HtmlNavNode {

        /**
         * Compat constructor for single-module callers: derives {@code payloadPath}
         * from the section id using the flat layout {@code classes/<sectionId>.data.js}.
         * Scoped callers should use the canonical constructor with an explicit path.
         */
        public File(String sectionId, String simpleName, double coveragePercent,
                    boolean hasCriticalInsight, boolean hasWarningInsight, boolean hasPositiveInsight) {
            this(sectionId,
                 "classes/" + sectionId + ".data.js",
                 simpleName,
                 coveragePercent,
                 hasCriticalInsight,
                 hasWarningInsight,
                 hasPositiveInsight);
        }
    }

    record ClassGroup(
        String sectionId,
        String payloadPath,
        String label,
        double coveragePercent,
        boolean hasCriticalInsight,
        boolean hasWarningInsight,
        boolean hasPositiveInsight,
        List<HtmlNavNode> children,
        boolean expandedByDefault
    ) implements HtmlNavNode {
        public ClassGroup {
            children = List.copyOf(children);
        }

        @Override
        public boolean isClassGroup() { return true; }
    }
}
