package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.TestTrackingSnapshot;
import io.github.kd656.coveragex.core.report.ReportScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReportModel {

    /** Synthetic scope id used by the single-module constructor. */
    public static final String SINGLE_MODULE_SCOPE_ID = "main";

    private final List<ReportScope> scopes;
    private final SummaryMetrics summaryMetrics;
    private final Map<Class<? extends StepResult>, StepResult> sections = new LinkedHashMap<>();
    private TestTrackingSnapshot testTrackingSnapshot = TestTrackingSnapshot.empty();

    /**
     * Single-module constructor. Wraps the class metrics in a synthetic {@code "main"} scope
     * so existing callers keep working unchanged.
     */
    public ReportModel(List<ClassMetrics> classMetrics, SummaryMetrics summaryMetrics) {
        this.scopes = List.of(new ReportScope(
                SINGLE_MODULE_SCOPE_ID,
                SINGLE_MODULE_SCOPE_ID,
                null,
                classMetrics,
                summaryMetrics
        ));
        this.summaryMetrics = summaryMetrics;
    }

    /**
     * Scope-aware factory used by multi-module aggregation. Uses a factory rather than
     * an overloaded constructor because {@code List<ClassMetrics>} and
     * {@code List<ReportScope>} erase to the same JVM signature.
     */
    public static ReportModel ofScopes(List<ReportScope> scopes, SummaryMetrics summaryMetrics) {
        return new ReportModel(scopes, summaryMetrics, null);
    }

    /** Private disambiguator so {@link #ofScopes} can call a distinct constructor. */
    private ReportModel(List<ReportScope> scopes, SummaryMetrics summaryMetrics, Void marker) {
        this.scopes = List.copyOf(scopes);
        this.summaryMetrics = summaryMetrics;
    }

    /**
     * Flat, unmodifiable view across every scope. Existing pipeline steps read from this;
     * post-refactor the list is a union across scopes.
     */
    public List<ClassMetrics> getClassMetrics() {
        List<ClassMetrics> flat = new ArrayList<>();
        for (ReportScope scope : scopes) {
            flat.addAll(scope.classMetrics());
        }
        return Collections.unmodifiableList(flat);
    }

    public List<ReportScope> getScopes() {
        return scopes;
    }

    /**
     * True when the model represents an aggregate over multiple scopes, or a single
     * scope with a non-default id. Views use this to switch between flat and
     * module-grouped layouts.
     */
    public boolean isScoped() {
        return scopes.size() > 1 || !SINGLE_MODULE_SCOPE_ID.equals(scopes.getFirst().scopeId());
    }

    public SummaryMetrics getSummaryMetrics() {
        return summaryMetrics;
    }

    public TestTrackingSnapshot getTestTrackingSnapshot() {
        return testTrackingSnapshot;
    }

    public void setTestTrackingSnapshot(TestTrackingSnapshot snapshot) {
        this.testTrackingSnapshot = snapshot;
    }

    public <T extends StepResult> void put(Class<T> type, T result) {
        sections.put(type, result);
    }

    public <T extends StepResult> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(sections.get(type)));
    }
}
