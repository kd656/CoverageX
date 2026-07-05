package io.github.kd656.coveragex.core.report.views.html;

import java.util.List;

/**
 * Top-level view model for the HTML report.
 *
 * <p>Single-module reports use the flat {@code navTree} and leave {@code modules}
 * empty; multi-module reports leave {@code navTree} empty and populate
 * {@code modules}. Templates branch on {@link #hasModules()} to decide which one
 * to render.</p>
 *
 * <p><b>Extension note:</b> fields may grow. Prefer accessor calls
 * ({@code report.navTree()}, {@code report.modules()}) over pattern matching so
 * future additions do not break call sites.</p>
 */
public record HtmlReportViewModel(
        HtmlSummary topBar,
        List<HtmlNavNode> navTree,
        List<HtmlModuleNode> modules
) {

    public HtmlReportViewModel {
        navTree = List.copyOf(navTree);
        modules = List.copyOf(modules);
    }

    /**
     * Compat constructor for the single-module path: {@code modules} defaults to
     * empty, preserving the pre-multi-module call sites.
     */
    public HtmlReportViewModel(HtmlSummary topBar, List<HtmlNavNode> navTree) {
        this(topBar, navTree, List.of());
    }

    public boolean hasModules() {
        return !modules.isEmpty();
    }
}
