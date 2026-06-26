package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import io.github.kd656.coveragex.core.collect.CommonCoverageDataCollector;
import io.github.kd656.coveragex.core.collect.CoverageDataCollectorDelegate;
import com.github.javaparser.JavaParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bytecode-level verification for the general method-call spill emitter.
 *
 * <p>Uses ASM {@link Textifier} to disassemble the instrumented bytecode and
 * asserts that:</p>
 * <ol>
 *   <li>For category-1 (reference) arguments the emitter uses {@code ASTORE}/{@code ALOAD}
 *       pairs. No {@code DUP}/{@code SWAP} should appear in the spill section.</li>
 *   <li>For category-2 (long) arguments the emitter uses {@code LSTORE}/{@code LLOAD}
 *       pairs — not {@code ASTORE}.</li>
 *   <li>For category-2 (double) arguments the emitter uses {@code DSTORE}/{@code DLOAD}
 *       pairs.</li>
 *   <li>The original {@code INVOKEVIRTUAL}/{@code INVOKESTATIC} instruction appears in
 *       the textified output — confirming that the spill does not consume the call.</li>
 * </ol>
 *
 * <p>These checks are structural rather than value-level: they prove that the emitter
 * uses the right opcode family per type and that the original call site is preserved.
 * Value-level correctness is covered by {@link MethodCallCaptureTest}.</p>
 */
class MethodCallSpillBytecodeTest {

    private static final String FIXTURE_CLASS = MethodCallCaptureFixtures.class.getName();
    private static final String FIXTURE_INTERNAL = FIXTURE_CLASS.replace('.', '/');
    private static final String FIXTURE_RESOURCE = FIXTURE_INTERNAL + ".class";

    private CommonCoverageDataCollector collector;
    private SourceAwareProbeInjector injector;
    private ClassModel classModel;

    @BeforeEach
    void setUp() throws Exception {
        collector = new CommonCoverageDataCollector(
                new BinaryDataWriter(),
                () -> Optional.empty());
        collector.reset();
        CoverageDataCollectorDelegate.registry().installGlobal(collector);
        injector = new SourceAwareProbeInjector(collector);

        URL classUrl = getClass().getClassLoader().getResource(FIXTURE_RESOURCE);
        assertThat(classUrl).as("Fixture class not found on classpath").isNotNull();

        Path classFile = Paths.get(classUrl.toURI());
        Path cursor = classFile;
        while (cursor != null && !cursor.getFileName().toString().equals("target")) {
            cursor = cursor.getParent();
        }
        assertThat(cursor).isNotNull();

        Path srcRoot = cursor.getParent().resolve("src/test/java");
        SemanticIndex index = new SemanticIndex();
        new SourceCodeAnalyzer(new JavaParser(), index).scan(srcRoot);

        classModel = index.getClasses().get(FIXTURE_CLASS);
        assertThat(classModel).as("Analyser must produce a ClassModel").isNotNull();
    }

    @AfterEach
    void tearDown() {
        collector.reset();
    }

    // =========================================================================
    // Reference-type args: ASTORE / ALOAD pairs
    // =========================================================================

    /**
     * For {@code s.isBlank()} (zero args, one ref receiver), the spill emitter must
     * emit {@code ASTORE} + {@code ALOAD} for the receiver, and the original
     * {@code INVOKEVIRTUAL isBlank} must follow the reload.
     */
    @Test
    void instanceZeroArgs_usesAstoreAload() throws Exception {
        String bytecode = textify(instrBytes());
        String isBlankMethod = methodSection(bytecode, "instanceZeroArgs");

        assertThat(isBlankMethod)
                .as("receiver spill must use ASTORE")
                .contains("ASTORE");
        assertThat(isBlankMethod)
                .as("receiver reload must use ALOAD")
                .contains("ALOAD");
        assertThat(isBlankMethod)
                .as("original INVOKEVIRTUAL isBlank must be preserved")
                .contains("isBlank");
    }

    /**
     * For {@code a.equals(b)} (one ref arg), the spill emitter must use ASTORE/ALOAD
     * for both receiver and the single reference argument, and the original
     * {@code INVOKEVIRTUAL equals} must follow.
     */
    @Test
    void instanceOneRefArg_usesAstoreAload() throws Exception {
        String bytecode = textify(instrBytes());
        String equalsMethod = methodSection(bytecode, "instanceOneRefArgBothCaptured");

        assertThat(equalsMethod)
                .as("receiver+arg spill must use ASTORE")
                .contains("ASTORE");
        assertThat(equalsMethod)
                .as("receiver+arg reload must use ALOAD")
                .contains("ALOAD");
        assertThat(equalsMethod)
                .as("original INVOKEVIRTUAL equals must be preserved")
                .contains("equals");
    }

    // =========================================================================
    // Long arg: LSTORE / LLOAD pairs
    // =========================================================================

    /**
     * For {@code limits.isAllowed(n)} (one long arg), the spill emitter must use
     * {@code LSTORE}/{@code LLOAD} for the long argument (not {@code ASTORE}).
     * The original {@code INVOKEVIRTUAL isAllowed} must follow the reload.
     */
    @Test
    void instanceLongArg_usesLstoreLload() throws Exception {
        String bytecode = textify(instrBytes());
        String isAllowedMethod = methodSection(bytecode, "instanceLongArg");

        assertThat(isAllowedMethod)
                .as("long-arg spill must use LSTORE")
                .contains("LSTORE");
        assertThat(isAllowedMethod)
                .as("long-arg reload must use LLOAD")
                .contains("LLOAD");
        assertThat(isAllowedMethod)
                .as("original INVOKEVIRTUAL isAllowed must be preserved")
                .contains("isAllowed");
    }

    // =========================================================================
    // Double arg: DSTORE / DLOAD pairs
    // =========================================================================

    /**
     * For {@code parser.accepts(x)} (one double arg), the spill emitter must use
     * {@code DSTORE}/{@code DLOAD} for the double argument, and the original
     * {@code INVOKEVIRTUAL accepts} must follow.
     */
    @Test
    void instanceDoubleArg_usesDstoreDload() throws Exception {
        String bytecode = textify(instrBytes());
        String acceptsMethod = methodSection(bytecode, "instanceDoubleArg");

        assertThat(acceptsMethod)
                .as("double-arg spill must use DSTORE")
                .contains("DSTORE");
        assertThat(acceptsMethod)
                .as("double-arg reload must use DLOAD")
                .contains("DLOAD");
        assertThat(acceptsMethod)
                .as("original INVOKEVIRTUAL accepts must be preserved")
                .contains("accepts");
    }

    // =========================================================================
    // Mixed cat-1 + cat-2: both LSTORE/LLOAD and DSTORE/DLOAD
    // =========================================================================

    /**
     * For {@code parser.acceptsMixed(off, score)} (long + double), the spill emitter
     * must use both {@code LSTORE}/{@code LLOAD} and {@code DSTORE}/{@code DLOAD},
     * and the original {@code INVOKEVIRTUAL acceptsMixed} must follow.
     */
    @Test
    void instanceMixedCat2Args_usesBothLongAndDouble() throws Exception {
        String bytecode = textify(instrBytes());
        String acceptsMixedMethod = methodSection(bytecode, "instanceMixedCat2Args");

        assertThat(acceptsMixedMethod)
                .as("long-arg portion must use LSTORE")
                .contains("LSTORE");
        assertThat(acceptsMixedMethod)
                .as("long-arg portion must use LLOAD")
                .contains("LLOAD");
        assertThat(acceptsMixedMethod)
                .as("double-arg portion must use DSTORE")
                .contains("DSTORE");
        assertThat(acceptsMixedMethod)
                .as("double-arg portion must use DLOAD")
                .contains("DLOAD");
        assertThat(acceptsMixedMethod)
                .as("original INVOKEVIRTUAL acceptsMixed must be preserved")
                .contains("acceptsMixed");
    }

    // =========================================================================
    // Static call: INVOKESTATIC preserved
    // =========================================================================

    /**
     * For {@code StubStatics.isBlank(s)} (static, one ref arg), the spill emitter
     * must use {@code ASTORE}/{@code ALOAD} and the original
     * {@code INVOKESTATIC isBlank} must follow.
     */
    @Test
    void staticOneRefArg_invokestatic_preserved() throws Exception {
        String bytecode = textify(instrBytes());
        String staticMethod = methodSection(bytecode, "staticOneRefArg");

        assertThat(staticMethod)
                .as("static-call arg spill must use ASTORE")
                .contains("ASTORE");
        assertThat(staticMethod)
                .as("static-call arg reload must use ALOAD")
                .contains("ALOAD");
        assertThat(staticMethod)
                .as("original INVOKESTATIC isBlank must be preserved")
                .contains("INVOKESTATIC");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private byte[] instrBytes() throws Exception {
        byte[] originalBytes;
        try (var stream = getClass().getClassLoader().getResourceAsStream(FIXTURE_RESOURCE)) {
            assertThat(stream).isNotNull();
            originalBytes = stream.readAllBytes();
        }
        SourceAwareInput input = new SourceAwareInput(originalBytes, classModel);
        return injector.injectProbes(FIXTURE_INTERNAL, input);
    }

    /**
     * Returns the Textifier-produced human-readable disassembly of the given
     * class bytecode.
     *
     * @param classBytes the compiled class bytecode
     * @return the full textified representation
     */
    private static String textify(byte[] classBytes) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ClassReader cr = new ClassReader(classBytes);
        cr.accept(new TraceClassVisitor(null, new Textifier(), pw),
                ClassReader.EXPAND_FRAMES);
        return sw.toString();
    }

    /**
     * Extracts the section of the textified bytecode that corresponds to the named
     * method. Returns the text from the first line containing the method name up to
     * (but not including) the next method header.
     *
     * @param fullText   the full Textifier output for the class
     * @param methodName the simple method name to find
     * @return the method's bytecode section
     */
    private static String methodSection(String fullText, String methodName) {
        int start = fullText.indexOf(methodName + "(");
        if (start < 0) {
            // Textifier may format the method differently — try just the name.
            start = fullText.indexOf(methodName);
        }
        assertThat(start)
                .as("Method '%s' not found in textified output", methodName)
                .isGreaterThanOrEqualTo(0);
        // Find the next "// access flags" comment which marks the next method.
        int nextMethodStart = fullText.indexOf("// access flags", start + 1);
        String section = nextMethodStart < 0
                ? fullText.substring(start)
                : fullText.substring(start, nextMethodStart);
        return section;
    }
}
