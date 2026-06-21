package io.github.kd656.coveragex.core.report.model;

import io.github.kd656.coveragex.api.data.TestTrackingSnapshot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReportModel {

    private final List<ClassMetrics> classMetrics;
    private final SummaryMetrics summaryMetrics;
    private final Map<Class<? extends StepResult>, StepResult> sections = new LinkedHashMap<>();
    private TestTrackingSnapshot testTrackingSnapshot = TestTrackingSnapshot.empty();

    public ReportModel(List<ClassMetrics> classMetrics, SummaryMetrics summaryMetrics) {
        this.classMetrics = classMetrics;
        this.summaryMetrics = summaryMetrics;
    }

    public List<ClassMetrics> getClassMetrics() { return Collections.unmodifiableList(classMetrics); }
    public SummaryMetrics getSummaryMetrics()   { return summaryMetrics; }

    public TestTrackingSnapshot getTestTrackingSnapshot() { return testTrackingSnapshot; }
    public void setTestTrackingSnapshot(TestTrackingSnapshot snapshot) { this.testTrackingSnapshot = snapshot; }

    public <T extends StepResult> void put(Class<T> type, T result) {
        sections.put(type, result);
    }

    public <T extends StepResult> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(type.cast(sections.get(type)));
    }
}
