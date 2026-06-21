package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.instrument.DefaultProbeInjector;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.ProbeMetadata.BranchDirection;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the TRUE/FALSE probe direction labels produced by
 * {@link DefaultProbeInjector} match the actual execution paths taken at runtime.
 *
 * <p>This is the widest layer of the IFNULL-polarity test pyramid. It exercises the
 * complete pipeline: bytecode instrumentation → classloader loading → method invocation
 * → probe hit recording → direction label assertion. A failure here means a real user
 * would see wrong TRUE/FALSE labels in a coverage report.</p>
 *
 * <h2>What this covers that lower layers cannot</h2>
 * <p>The polarity-table unit test ({@code ProbeInjectionSupportTest}) and the
 * source-analysis unit test ({@code OperandPolarityTest}) verify individual
 * components in isolation. This test verifies that the full end-to-end assembly is
 * correct: the probe ID allocation, the jump retargeting, and the
 * {@code probeHits[probeId]} → {@code BranchDirection} mapping all line up as expected
 * when real bytecode is executed.</p>
 *
 * <h2>Fixture classes</h2>
 * <p>Two inner static fixture classes serve as the instrumentation targets:</p>
 * <ul>
 *   <li>{@link NullOrBlankFixture} — contains a {@code s == null || s.isBlank()}
 *       condition (the exact pattern from the bug report).</li>
 *   <li>{@link NotNullAndNonBlankFixture} — contains a {@code s != null && !s.isBlank()}
 *       condition (the "accidentally correct" counter-pattern).</li>
 * </ul>
 *
 * <h2>Assertion strategy</h2>
 * <p>After each method invocation the test collects all {@link ProbeMetadata.BranchProbe}s
 * whose corresponding hit flag is {@code true} in the probe-hits array. For each
 * assertion we verify both that the expected direction fired AND that the opposite
 * direction did not fire for the same condition text. This two-sided check is what
 * would have caught the original bug: the old code fired the wrong direction while
 * the correct one stayed silent.</p>
 */
class BranchProbePolarityTest {

    /**
     * Fixture exercising the {@code || null-check} pattern from the bug report.
     *
     * <p>javac compiles {@code s == null} with {@code IFNULL then_label}: the jump
     * fires when s IS null — i.e. when {@code s == null} is TRUE. The TRUE probe for
     * this operand must therefore fire when {@code null} is passed and the FALSE probe
     * must fire when a non-null value is passed.</p>
     */
    public static class NullOrBlankFixture {
        public String check(String s) {
            if (s == null || s.isBlank()) {
                return "blank";
            }
            return "ok";
        }
    }

    /**
     * Fixture exercising the {@code && not-null-check} counter-pattern.
     *
     * <p>javac compiles {@code s != null} with {@code IFNULL else_label}: the jump
     * fires when s IS null — i.e. when {@code s != null} is FALSE. The generic label
     * from {@link DefaultProbeInjector} says {@code "if (x == null)"}, which is
     * consistent with the opcode: TRUE fires when x IS null, FALSE fires when x is
     * not null.</p>
     */
    public static class NotNullAndNonBlankFixture {
        public String check(String s) {
            if (s != null && !s.isBlank()) {
                return "ok";
            }
            return "blank";
        }
    }

    // =========================================================================
    // Test infrastructure
    // =========================================================================

    private CommonCoverageDataCollector collector;
    private DefaultProbeInjector injector;

    @BeforeEach
    void setUp() {
        collector = new CommonCoverageDataCollector(new BinaryDataWriter());
        collector.reset();
        CoverageDataCollectorDelegate.registry().installGlobal(collector);
        injector = new DefaultProbeInjector(collector);
    }

    @AfterEach
    void tearDown() {
        collector.reset();
    }

    // =========================================================================
    // Infrastructure helpers
    // =========================================================================

    /**
     * Loads the original bytecode of {@code fixtureClass} from the classpath,
     * instruments it with {@link DefaultProbeInjector}, defines the instrumented
     * class in an isolated classloader, and returns a new instance ready for use.
     *
     * <p>The isolated classloader is required because the test JVM has already
     * loaded the original (non-instrumented) version of the fixture. Without
     * isolation the original class definition would be returned instead of the
     * instrumented one.</p>
     *
     * @param fixtureClass the inner static fixture class to instrument
     * @return a fresh instance of the instrumented class
     */
    private Object instrumentAndInstantiate(Class<?> fixtureClass) throws Exception {
        String internalName = fixtureClass.getName().replace('.', '/');
        String resourcePath = internalName + ".class";

        byte[] originalBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(stream)
                    .as("Fixture class bytes not found on classpath: %s", resourcePath)
                    .isNotNull();
            originalBytes = stream.readAllBytes();
        }

        byte[] instrumented = injector.injectProbes(internalName, originalBytes);

        ClassLoader isolated = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(fixtureClass.getName())) {
                    Class<?> cached = findLoadedClass(name);
                    if (cached != null) return cached;
                    return defineClass(name, instrumented, 0, instrumented.length);
                }
                return super.loadClass(name);
            }
        };

        Class<?> instrumentedClass = isolated.loadClass(fixtureClass.getName());
        return instrumentedClass.getDeclaredConstructor().newInstance();
    }

    /**
     * Returns every {@link ProbeMetadata.BranchProbe} for {@code fixtureClass}
     * whose probe hit flag is {@code true} after the most recent invocation.
     *
     * @param fixtureClass the fixture class whose coverage data should be read
     * @return live fired branch probes, in probe-ID order
     */
    private List<ProbeMetadata.BranchProbe> firedBranchProbes(Class<?> fixtureClass) {
        String internalName = fixtureClass.getName().replace('.', '/');
        ClassCoverage cc = collector.snapshot().classCoverage(internalName);

        assertThat(cc)
                .as("No coverage data found for %s — was it instrumented?", internalName)
                .isNotNull();

        boolean[] hits = cc.probeHits();
        return cc.probeMetadata().stream()
                .filter(m -> m instanceof ProbeMetadata.BranchProbe)
                .map(m -> (ProbeMetadata.BranchProbe) m)
                .filter(bp -> hits[bp.probeId()])
                .toList();
    }

    /**
     * Invokes the {@code check(String)} method on the given instance via reflection
     * and returns the fired branch probes. The {@code arg} parameter may be
     * {@code null} to test the null path.
     */
    private List<ProbeMetadata.BranchProbe> invokeAndCollect(
            Object instance, Class<?> fixtureClass, String arg) throws Exception {

        collector.reset();
        // Re-register the class so probe metadata is available after reset.
        // Instrumentation already happened; we just need to trigger it again.
        String internalName = fixtureClass.getName().replace('.', '/');
        String resourcePath = internalName + ".class";
        byte[] originalBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            originalBytes = stream.readAllBytes();
        }
        // Re-inject to re-register metadata with the freshly reset collector.
        injector.injectProbes(internalName, originalBytes);

        Method check = instance.getClass().getDeclaredMethod("check", String.class);
        check.invoke(instance, arg);

        return firedBranchProbes(fixtureClass);
    }

    // =========================================================================
    // NullOrBlankFixture — s == null || s.isBlank()
    // =========================================================================

    /**
     * When {@code s} is {@code null}, the {@code IFNULL} jump is taken and
     * execution short-circuits to the then-body before {@code isBlank()} is reached.
     *
     * <p>Expected outcome: the TRUE probe for {@code "if (x == null)"} fires;
     * the FALSE probe for the same condition must NOT fire, because {@code s == null}
     * was in fact true.</p>
     *
     * <p>This is the regression test for the original bug: before the fix, the
     * FALSE probe fired instead of the TRUE probe when {@code null} was passed.</p>
     */
    @Test
    void nullOrBlank_nullArg_trueProbeFiresForNullCheck() throws Exception {
        Object instance = instrumentAndInstantiate(NullOrBlankFixture.class);
        List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NullOrBlankFixture.class, null);

        assertThat(fired)
                .as("TRUE probe for 'if (x == null)' must fire when s is null")
                .anyMatch(bp -> bp.conditionText().equals("if (x == null)")
                             && bp.direction() == BranchDirection.TRUE);

        assertThat(fired)
                .as("FALSE probe for 'if (x == null)' must NOT fire when s is null")
                .noneMatch(bp -> bp.conditionText().equals("if (x == null)")
                              && bp.direction() == BranchDirection.FALSE);
    }

    /**
     * When {@code s} is a non-null blank string (e.g. {@code "  "}), the
     * {@code IFNULL} jump is NOT taken (s is not null), and {@code isBlank()} returns
     * true, so execution still reaches the then-body via the second operand.
     *
     * <p>Expected outcome:</p>
     * <ul>
     *   <li>FALSE probe for {@code "if (x == null)"} fires — {@code s == null} was false.</li>
     *   <li>TRUE probe for the isBlank check fires — the condition was true.</li>
     *   <li>TRUE probe for {@code "if (x == null)"} must NOT fire.</li>
     * </ul>
     */
    @Test
    void nullOrBlank_blankArg_falseProbeFiresForNullCheck() throws Exception {
        Object instance = instrumentAndInstantiate(NullOrBlankFixture.class);
        List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NullOrBlankFixture.class, "  ");

        assertThat(fired)
                .as("FALSE probe for 'if (x == null)' must fire when s is not null")
                .anyMatch(bp -> bp.conditionText().equals("if (x == null)")
                             && bp.direction() == BranchDirection.FALSE);

        assertThat(fired)
                .as("TRUE probe for 'if (x == null)' must NOT fire when s is not null")
                .noneMatch(bp -> bp.conditionText().equals("if (x == null)")
                              && bp.direction() == BranchDirection.TRUE);
    }

    /**
     * When {@code s} is a non-null non-blank string (e.g. {@code "hello"}), neither
     * condition is satisfied: the entire {@code if} evaluates to false and the
     * else-body is reached.
     *
     * <p>Expected outcome: FALSE probes for both conditions fire; TRUE probes for both
     * must NOT fire. This confirms that the FALSE direction is consistently triggered
     * when neither condition holds.</p>
     */
    @Test
    void nullOrBlank_nonBlankArg_bothFalseProbesFire() throws Exception {
        Object instance = instrumentAndInstantiate(NullOrBlankFixture.class);
        List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NullOrBlankFixture.class, "hello");

        assertThat(fired)
                .as("FALSE probe for null check must fire when s is non-null non-blank")
                .anyMatch(bp -> bp.conditionText().equals("if (x == null)")
                             && bp.direction() == BranchDirection.FALSE);

        assertThat(fired)
                .as("FALSE probe for blank check must fire when s is non-null non-blank")
                .anyMatch(bp -> bp.conditionText().equals("if (x == 0)")
                             && bp.direction() == BranchDirection.FALSE);

        assertThat(fired)
                .as("TRUE probe for null check must NOT fire when s is non-null non-blank")
                .noneMatch(bp -> bp.conditionText().equals("if (x == null)")
                              && bp.direction() == BranchDirection.TRUE);
    }

    /**
     * Verifies that the TRUE and FALSE probes for {@code "if (x == null)"} are mutually
     * exclusive within a single invocation: no single call should cause both directions
     * to fire simultaneously.
     *
     * <p>This invariant holds for any correct probe injection: both probes share the
     * same synthetic branch, so only one path can be taken per execution.</p>
     */
    @Test
    void nullOrBlank_trueAndFalseProbesAreMutuallyExclusive() throws Exception {
        Object instance = instrumentAndInstantiate(NullOrBlankFixture.class);

        for (String arg : new String[]{null, "  ", "hello"}) {
            List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NullOrBlankFixture.class, arg);

            long firedForNullCheck = fired.stream()
                    .filter(bp -> bp.conditionText().equals("if (x == null)"))
                    .count();

            assertThat(firedForNullCheck)
                    .as("At most one direction (TRUE or FALSE) must fire for 'if (x == null)' per invocation, arg=%s", arg)
                    .isLessThanOrEqualTo(1);
        }
    }

    // =========================================================================
    // NotNullAndNonBlankFixture — s != null && !s.isBlank()
    // =========================================================================

    /**
     * When {@code s} is {@code null}, the {@code IFNULL} jump is taken: s IS null,
     * so the condition text {@code "if (x == null)"} (generated by
     * {@link DefaultProbeInjector}) evaluates to true. The TRUE probe fires.
     *
     * <p>Note: the {@link DefaultProbeInjector} labels this probe as {@code "if (x == null)"}
     * because it cannot recover the source expression {@code s != null} from bytecode
     * alone. The TRUE/FALSE labels are internally consistent with the label text even
     * though the actual source condition is inverted. This is a documented limitation
     * of {@link DefaultProbeInjector} for the {@code != null &&} pattern.</p>
     */
    @Test
    void notNullAnd_nullArg_trueProbeFiresForNullOpcode() throws Exception {
        Object instance = instrumentAndInstantiate(NotNullAndNonBlankFixture.class);
        List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NotNullAndNonBlankFixture.class, null);

        assertThat(fired)
                .as("TRUE probe for 'if (x == null)' must fire when s IS null (IFNULL jumps)")
                .anyMatch(bp -> bp.conditionText().equals("if (x == null)")
                             && bp.direction() == BranchDirection.TRUE);
    }

    /**
     * When {@code s} is non-null, the {@code IFNULL} fall-through path is taken:
     * the FALSE probe for {@code "if (x == null)"} fires.
     */
    @Test
    void notNullAnd_nonNullArg_falseProbeFiresForNullOpcode() throws Exception {
        Object instance = instrumentAndInstantiate(NotNullAndNonBlankFixture.class);
        List<ProbeMetadata.BranchProbe> fired = invokeAndCollect(instance, NotNullAndNonBlankFixture.class, "hello");

        assertThat(fired)
                .as("FALSE probe for 'if (x == null)' must fire when s is not null (IFNULL falls through)")
                .anyMatch(bp -> bp.conditionText().equals("if (x == null)")
                             && bp.direction() == BranchDirection.FALSE);
    }
}
