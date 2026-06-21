package com.coveragex.core.report;

import com.coveragex.core.report.model.ReportingType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ReportingTypeParser {
    private ReportingTypeParser() {}

    public static Set<ReportingType> parse(Collection<String> formats) {
        Set<ReportingType> result = new LinkedHashSet<>();
        for (String f : formats) {
            if (f == null || f.isBlank()) continue;
            try {
                result.add(ReportingType.valueOf(f.strip().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }
}
