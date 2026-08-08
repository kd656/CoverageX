package io.github.kd656.coveragex.core.multi;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.core.report.ReportInput;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleClassOwnershipIndexRoutingTest {

    @Test
    void reassignsBorrowedClassesToOwnerScope() {
        ReportInput dto = input("dto", Map.of());
        ReportInput service = input("service", Map.of(
            "com/example/dto/UserRecord",     coverage("com/example/dto/UserRecord",     new boolean[]{true}),
            "com/example/service/UserService", coverage("com/example/service/UserService", new boolean[]{true})));

        ModuleClassOwnershipIndex ownership = ownershipFor(Map.of(
            "com/example/dto/UserRecord",     "dto",
            "com/example/service/UserService", "service"));

        List<ReportInput> routed = ownership.route(List.of(dto, service));

        assertThat(findScope(routed, "dto").executionData().classes().keySet())
            .containsExactly("com/example/dto/UserRecord");
        assertThat(findScope(routed, "service").executionData().classes().keySet())
            .containsExactly("com/example/service/UserService");
    }

    @Test
    void unknownClassFallsBackToSourceInput() {
        ReportInput service = input("service", Map.of(
            "com/example/runtime/Generated", coverage("com/example/runtime/Generated", new boolean[]{true})));

        List<ReportInput> routed = ModuleClassOwnershipIndex.empty().route(List.of(service));

        assertThat(routed).hasSize(1);
        assertThat(routed.getFirst().executionData().classes().keySet())
            .containsExactly("com/example/runtime/Generated");
    }

    @Test
    void mergesSameFqcnRecordedInMultipleScopes() {
        boolean[] serviceHits = {true, true, false, false};
        boolean[] apiHits     = {false, false, true, true};

        ReportInput dto     = input("dto", Map.of());
        ReportInput service = input("service", Map.of(
            "com/example/dto/UserRecord", coverage("com/example/dto/UserRecord", serviceHits)));
        ReportInput api     = input("api", Map.of(
            "com/example/dto/UserRecord", coverage("com/example/dto/UserRecord", apiHits)));

        ModuleClassOwnershipIndex ownership = ownershipFor(Map.of(
            "com/example/dto/UserRecord", "dto"));

        List<ReportInput> routed = ownership.route(List.of(dto, service, api));

        ClassCoverage merged = findScope(routed, "dto").executionData().classes().get("com/example/dto/UserRecord");
        assertThat(merged.probeHits()).containsExactly(true, true, true, true);
    }

    @Test
    void preservesScopeOrderFromRawInputs() {
        ReportInput a = input("api", Map.of());
        ReportInput s = input("service", Map.of());
        ReportInput d = input("dto", Map.of());

        List<ReportInput> routed = ModuleClassOwnershipIndex.empty().route(List.of(a, s, d));

        assertThat(routed).extracting(ReportInput::scopeId).containsExactly("api", "service", "dto");
    }

    @Test
    void unknownOwnerNotInInputsFallsBackToSourceScope() {
        ReportInput service = input("service", Map.of(
            "com/example/dto/UserRecord", coverage("com/example/dto/UserRecord", new boolean[]{true})));

        ModuleClassOwnershipIndex ownership = ownershipFor(Map.of(
            "com/example/dto/UserRecord", "dto"));

        List<ReportInput> routed = ownership.route(List.of(service));

        assertThat(routed.getFirst().executionData().classes().keySet())
            .containsExactly("com/example/dto/UserRecord");
    }

    private static ReportInput input(String scopeId, Map<String, ClassCoverage> classes) {
        Map<String, ClassCoverage> preserved = new LinkedHashMap<>(classes);
        return new ReportInput(scopeId, scopeId, null, new ExecutionData(preserved), Map.of());
    }

    private static ClassCoverage coverage(String classId, boolean[] probeHits) {
        return new ClassCoverage(classId, probeHits, Map.of(), Map.of(), List.of(), null);
    }

    private static ReportInput findScope(List<ReportInput> inputs, String scopeId) {
        for (ReportInput input : inputs) {
            if (input.scopeId().equals(scopeId)) {
                return input;
            }
        }
        throw new AssertionError("scope not found: " + scopeId);
    }

    private static ModuleClassOwnershipIndex ownershipFor(Map<String, String> ownershipMap) {
        return new ModuleClassOwnershipIndex(ownershipMap);
    }
}
