package io.github.kd656.coveragex.compat.testutil;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads compiled fixture class bytes from the test classpath.
 *
 * <p>Fixtures are produced by the {@code coveragex-test-fixtures} module and made
 * available as a test-scope jar. {@link #load(String)} reads the raw bytecode by
 * fully-qualified name (e.g. {@code "io.github.kd656.coveragex.fixtures.IfElse"}).</p>
 *
 * <p>Includes a fail-fast diagnostic for class file version skew: if a fixture
 * was compiled by a JDK whose class file major version exceeds what the bundled
 * ASM can read, the failure message names the JDK and points at the fix. Saves
 * an hour of head-scratching when a future matrix row trips this.</p>
 */
public final class BytecodeFixtures {

    /** Bump alongside ASM upgrades. ASM 9.7.x supports class file major 69 (JDK 25). */
    private static final int ASM_MAX_CLASS_FILE_MAJOR = 69;

    /**
     * Loads the fixture plus every nested class declared in its {@code InnerClasses}
     * attribute. Returned map keys are JVM internal names (e.g. {@code "io/github/kd656/coveragex/fixtures/TryWithResources$AutoCloseableImpl"})
     * suitable for use with {@link ByteArrayClassLoader}.
     *
     * <p>Required when a fixture references its own nested types at runtime — without
     * loading both into the same classloader, the JVM treats them as
     * different runtime packages and throws {@code IllegalAccessError}.</p>
     */
    public static Map<String, byte[]> loadWithNestedClasses(String fixtureFqn) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        String classId = fixtureFqn.replace('.', '/');
        byte[] outerBytes = load(fixtureFqn);
        result.put(classId, outerBytes);

        new ClassReader(outerBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                if (name == null || result.containsKey(name)) return;
                // Only follow nested classes of *this* fixture — InnerClasses also
                // lists any nested types the outer happens to reference (e.g. Map.Entry).
                if (!name.startsWith(classId + "$")) return;
                result.put(name, load(name.replace('/', '.')));
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return result;
    }

    public static byte[] load(String fixtureFqn) {
        String resource = "/" + fixtureFqn.replace('.', '/') + ".class";
        try (var in = BytecodeFixtures.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture on classpath: " + resource
                        + " — verify the active 'fixtures-jdkNN' profile includes the right fixtures module.");
            }
            byte[] bytes = in.readAllBytes();
            checkClassFileVersion(bytes, fixtureFqn);
            return bytes;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void checkClassFileVersion(byte[] bytes, String fqn) {
        if (bytes.length < 8) {
            throw new IllegalStateException("Truncated class file: " + fqn);
        }
        int major = ByteBuffer.wrap(bytes, 6, 2).order(ByteOrder.BIG_ENDIAN).getShort() & 0xFFFF;
        if (major > ASM_MAX_CLASS_FILE_MAJOR) {
            throw new IllegalStateException(String.format(
                    "Fixture %s compiled to class file major %d; ASM in this build supports up to %d. "
                            + "Upgrade ASM (and bump ASM_MAX_CLASS_FILE_MAJOR in BytecodeFixtures).",
                    fqn, major, ASM_MAX_CLASS_FILE_MAJOR));
        }
    }

    private BytecodeFixtures() {}
}
