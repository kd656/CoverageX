package io.github.kd656.coveragex.compat.testutil;

import io.github.kd656.coveragex.core.probe.ProbePlan;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JUnit extension that dumps captured probe plans to {@code target/plan-dumps/jdk-N/}
 * when a test fails. Never on success — keeps the build tree clean.
 *
 * <p>Test code calls {@link #capture(String, ProbePlan)} after building a plan.
 * If the test then fails, the captured plans are written as line-oriented text
 * via {@link PlanFormatter}. CI uploads the dump directory as a build artefact
 * (see {@code .github/workflows/ci.yml}).</p>
 *
 * <p>Lifecycle note: TestWatcher callbacks run <em>after</em> {@code AfterEachCallback}.
 * We therefore clear the per-thread capture map from the TestWatcher callbacks themselves,
 * not from {@code afterEach} — otherwise the success path would clear the map before
 * the failure path could read it.</p>
 */
public final class PlanDumpOnFailure implements TestWatcher {

    private static final ThreadLocal<Map<String, ProbePlan>> CAPTURED =
            ThreadLocal.withInitial(LinkedHashMap::new);

    public static void capture(String fixture, ProbePlan plan) {
        CAPTURED.get().put(fixture, plan);
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        Path dir = Path.of("target/plan-dumps", "jdk-" + JdkVersion.current());
        try {
            Files.createDirectories(dir);
            for (var entry : CAPTURED.get().entrySet()) {
                Files.writeString(dir.resolve(entry.getKey() + ".plan.txt"),
                        PlanFormatter.format(entry.getValue()));
            }
        } catch (IOException ignored) {
            // Dumps are best-effort triage material; never let an I/O failure mask the real assertion error.
        } finally {
            CAPTURED.get().clear();
        }
    }

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        CAPTURED.get().clear();
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        CAPTURED.get().clear();
    }

    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        CAPTURED.get().clear();
    }
}
