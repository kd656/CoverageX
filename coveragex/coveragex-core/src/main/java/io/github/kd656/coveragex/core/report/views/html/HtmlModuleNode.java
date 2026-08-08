package io.github.kd656.coveragex.core.report.views.html;

import java.util.List;

/**
 * One module in the sidebar of a scoped (multi-module) HTML report.
 *
 * <p>A module wraps the existing package/class {@link HtmlNavNode} tree with an
 * outer level so the sidebar can group classes by scope. Single-module reports
 * do not emit these nodes at all.</p>
 *
 * <p><b>Extension note:</b> fields may grow. Prefer accessors over pattern
 * matching so future additions do not break call sites.</p>
 */
public record HtmlModuleNode(
        String scopeId,
        String displayName,
        double coveragePercent,
        int classCount,
        boolean hasCriticalInsight,
        boolean hasWarningInsight,
        List<HtmlNavNode> navTree,
        boolean expandedByDefault
) {
    public HtmlModuleNode {
        navTree = List.copyOf(navTree);
    }
}
