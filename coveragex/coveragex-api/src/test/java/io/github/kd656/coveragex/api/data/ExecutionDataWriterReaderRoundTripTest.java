package io.github.kd656.coveragex.api.data;

import io.github.kd656.coveragex.api.io.internal.BinaryDataReader;
import io.github.kd656.coveragex.api.io.internal.BinaryDataWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Round-trip tests for {@link BinaryDataWriter} and {@link BinaryDataReader}.
 *
 * <p>Each test writes via BinaryDataWriter and reads back via BinaryDataReader,
 * asserting field equality to confirm the binary format is self-consistent.</p>
 */
class ExecutionDataWriterReaderRoundTripTest {

    private final BinaryDataReader coverageDataReader = new BinaryDataReader();
    private final BinaryDataWriter coverageDataWriter = new BinaryDataWriter();

    // -------------------------------------------------------------------------
    // Probe hits
    // -------------------------------------------------------------------------

    @Test
    void probeHits_allFalse_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false, false, false}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classes().get("com/example/Foo").probeHits())
                .containsExactly(false, false, false);
    }

    @Test
    void probeHits_mixedTrueFalse_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true, false, true, false, true}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classes().get("com/example/Foo").probeHits())
                .containsExactly(true, false, true, false, true);
    }

    @Test
    void emptyClassList_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        coverageDataWriter.write(file, new ExecutionData(List.of()));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classCount()).isEqualTo(0);
        assertThat(result.totalProbes()).isEqualTo(0);
        assertThat(result.executedProbes()).isEqualTo(0);
        assertThat(result.probeCoveragePercent()).isEqualTo(0.0);
    }

    // -------------------------------------------------------------------------
    // Probe metadata — each type
    // -------------------------------------------------------------------------

    @Test
    void metadata_methodProbe_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ProbeMetadata.MethodProbe probe = new ProbeMetadata.MethodProbe(0, "doWork", 10, 20);
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false}, Map.of(), Map.of(), List.of(probe), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ProbeMetadata readProbe = result.classes().get("com/example/Foo").probeMetadata().get(0);
        assertThat(readProbe).isInstanceOf(ProbeMetadata.MethodProbe.class);
        ProbeMetadata.MethodProbe mp = (ProbeMetadata.MethodProbe) readProbe;
        assertThat(mp.probeId()).isEqualTo(0);
        assertThat(mp.methodName()).isEqualTo("doWork");
        assertThat(mp.startLine()).isEqualTo(10);
        assertThat(mp.endLine()).isEqualTo(20);
    }

    @Test
    void metadata_branchProbe_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ProbeMetadata.BranchProbe probe = new ProbeMetadata.BranchProbe(
                1, "check", 15, "if (x == null)", ProbeMetadata.BranchDirection.TRUE);
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false, false}, Map.of(), Map.of(), List.of(
                        new ProbeMetadata.MethodProbe(0, "check", 10, 20), probe), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ProbeMetadata readProbe = result.classes().get("com/example/Foo").probeMetadata().get(1);
        assertThat(readProbe).isInstanceOf(ProbeMetadata.BranchProbe.class);
        ProbeMetadata.BranchProbe bp = (ProbeMetadata.BranchProbe) readProbe;
        assertThat(bp.probeId()).isEqualTo(1);
        assertThat(bp.methodName()).isEqualTo("check");
        assertThat(bp.line()).isEqualTo(15);
        assertThat(bp.conditionText()).isEqualTo("if (x == null)");
        assertThat(bp.direction()).isEqualTo(ProbeMetadata.BranchDirection.TRUE);
    }

    @Test
    void metadata_returnProbe_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ProbeMetadata.ReturnProbe probe = new ProbeMetadata.ReturnProbe(0, "run", 42);
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false}, Map.of(), Map.of(), List.of(probe), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ProbeMetadata readProbe = result.classes().get("com/example/Foo").probeMetadata().get(0);
        assertThat(readProbe).isInstanceOf(ProbeMetadata.ReturnProbe.class);
        ProbeMetadata.ReturnProbe rp = (ProbeMetadata.ReturnProbe) readProbe;
        assertThat(rp.probeId()).isEqualTo(0);
        assertThat(rp.methodName()).isEqualTo("run");
        assertThat(rp.line()).isEqualTo(42);
    }

    @Test
    void metadata_throwProbe_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ProbeMetadata.ThrowProbe probe = new ProbeMetadata.ThrowProbe(0, "run", 42);
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false}, Map.of(), Map.of(), List.of(probe), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ProbeMetadata readProbe = result.classes().get("com/example/Foo").probeMetadata().get(0);
        assertThat(readProbe).isInstanceOf(ProbeMetadata.ThrowProbe.class);
        ProbeMetadata.ThrowProbe tp = (ProbeMetadata.ThrowProbe) readProbe;
        assertThat(tp.probeId()).isEqualTo(0);
        assertThat(tp.methodName()).isEqualTo("run");
        assertThat(tp.line()).isEqualTo(42);
    }

    @Test
    void metadata_segmentProbe_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ProbeMetadata.SegmentProbe probe = new ProbeMetadata.SegmentProbe(0, "process", 5, 15);
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{false}, Map.of(), Map.of(), List.of(probe), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ProbeMetadata readProbe = result.classes().get("com/example/Foo").probeMetadata().get(0);
        assertThat(readProbe).isInstanceOf(ProbeMetadata.SegmentProbe.class);
        ProbeMetadata.SegmentProbe sp = (ProbeMetadata.SegmentProbe) readProbe;
        assertThat(sp.probeId()).isEqualTo(0);
        assertThat(sp.methodName()).isEqualTo("process");
        assertThat(sp.startLine()).isEqualTo(5);
        assertThat(sp.endLine()).isEqualTo(15);
    }

    @Test
    void metadata_noMetadata_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classes().get("com/example/Foo").probeMetadata()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Method hits
    // -------------------------------------------------------------------------

    @Test
    void methodHits_singleInvocationNoArgs_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        InvocationRecord inv = new InvocationRecord(List.of(), 1);
        MethodHit hit = new MethodHit("doWork", List.of(inv));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(0, hit), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        MethodHit readHit = result.classes().get("com/example/Foo").methodHits().get(0);
        assertThat(readHit.methodName()).isEqualTo("doWork");
        assertThat(readHit.invocations()).hasSize(1);
        assertThat(readHit.invocations().get(0).args()).isEmpty();
        assertThat(readHit.invocations().get(0).count()).isEqualTo(1);
    }

    @Test
    void methodHits_multipleInvocations_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        InvocationRecord inv1 = new InvocationRecord(List.of("alpha"), 3);
        InvocationRecord inv2 = new InvocationRecord(List.of("beta"), 2);
        MethodHit hit = new MethodHit("process", List.of(inv1, inv2));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(0, hit), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        MethodHit readHit = result.classes().get("com/example/Foo").methodHits().get(0);
        assertThat(readHit.invocations()).hasSize(2);

        boolean foundAlpha = false, foundBeta = false;
        for (InvocationRecord rec : readHit.invocations()) {
            if (rec.args().equals(List.of("alpha"))) {
                assertThat(rec.count()).isEqualTo(3);
                foundAlpha = true;
            } else if (rec.args().equals(List.of("beta"))) {
                assertThat(rec.count()).isEqualTo(2);
                foundBeta = true;
            }
        }
        assertThat(foundAlpha).as("alpha invocation not found").isTrue();
        assertThat(foundBeta).as("beta invocation not found").isTrue();
    }

    @Test
    void methodHits_nullableArgs_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        List<String> argsWithNull = new java.util.ArrayList<>();
        argsWithNull.add(null);
        InvocationRecord invWithNull = new InvocationRecord(argsWithNull, 1);
        InvocationRecord invWithNullString = new InvocationRecord(List.of("null"), 1);
        MethodHit hit = new MethodHit("check", List.of(invWithNull, invWithNullString));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(0, hit), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        MethodHit readHit = result.classes().get("com/example/Foo").methodHits().get(0);
        assertThat(readHit.invocations()).hasSize(2);

        boolean foundActualNull = false, foundNullString = false;
        for (InvocationRecord rec : readHit.invocations()) {
            if (rec.args().size() == 1 && rec.args().get(0) == null) {
                foundActualNull = true;
            } else if (rec.args().size() == 1 && "null".equals(rec.args().get(0))) {
                foundNullString = true;
            }
        }
        assertThat(foundActualNull).as("null arg (real null) not found after round-trip").isTrue();
        assertThat(foundNullString).as("\"null\" string arg not found after round-trip").isTrue();
    }

    @Test
    void methodHits_noMethodHits_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true, false}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classes().get("com/example/Foo").methodHits()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // ProbeHit counters
    // -------------------------------------------------------------------------

    @Test
    void hits_multipleEntries_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        Map<Integer, ProbeHit> hits = Map.of(
            0, new ProbeHit(0, 1),
            2, new ProbeHit(2, 42),
            5, new ProbeHit(5, 1_000_000)
        );
        ClassCoverage cc = new ClassCoverage(
                "com/example/Foo",
                new boolean[]{true, false, true, false, false, true},
                Map.of(),
                hits,
                List.of(),
                null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        Map<Integer, ProbeHit> readHits = result.classes().get("com/example/Foo").hits();
        assertThat(readHits).containsOnlyKeys(0, 2, 5);
        assertThat(readHits.get(0).count()).isEqualTo(1);
        assertThat(readHits.get(2).count()).isEqualTo(42);
        assertThat(readHits.get(5).count()).isEqualTo(1_000_000);
    }

    @Test
    void hits_emptyMap_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage(
                "com/example/Foo",
                new boolean[]{false, false},
                Map.of(),
                Map.of(),
                List.of(),
                null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classes().get("com/example/Foo").hits()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Test attribution
    // -------------------------------------------------------------------------

    @Test
    void testAttribution_singleProbeSingleTest_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        AttributedInvocation attrInv = new AttributedInvocation(List.of("arg1"), List.of("testFoo"));
        ClassTestCoverage attribution = new ClassTestCoverage("com/example/Foo",
                Map.of(0, List.of(attrInv)));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(), Map.of(), List.of(), attribution);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ClassTestCoverage readAttribution = result.classes().get("com/example/Foo").testAttribution();
        assertThat(readAttribution.probeInvocations()).containsKey(0);
        List<AttributedInvocation> invocations = readAttribution.probeInvocations().get(0);
        assertThat(invocations).hasSize(1);
        assertThat(invocations.get(0).args()).containsExactly("arg1");
        assertThat(invocations.get(0).testMethods()).containsExactly("testFoo");
    }

    @Test
    void testAttribution_multipleProbesMultipleTests_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        AttributedInvocation inv0 = new AttributedInvocation(List.of("a"), List.of("testA", "testB"));
        AttributedInvocation inv1 = new AttributedInvocation(List.of("b"), List.of("testC"));
        ClassTestCoverage attribution = new ClassTestCoverage("com/example/Foo",
                Map.of(0, List.of(inv0), 1, List.of(inv1)));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true, true}, Map.of(), Map.of(), List.of(), attribution);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ClassTestCoverage readAttr = result.classes().get("com/example/Foo").testAttribution();
        assertThat(readAttr.probeInvocations()).hasSize(2);

        List<AttributedInvocation> probe0 = readAttr.probeInvocations().get(0);
        assertThat(probe0).hasSize(1);
        assertThat(probe0.get(0).args()).containsExactly("a");
        assertThat(probe0.get(0).testMethods()).containsExactlyInAnyOrder("testA", "testB");

        List<AttributedInvocation> probe1 = readAttr.probeInvocations().get(1);
        assertThat(probe1).hasSize(1);
        assertThat(probe1.get(0).args()).containsExactly("b");
        assertThat(probe1.get(0).testMethods()).containsExactly("testC");
    }

    @Test
    void testAttribution_nullableArgs_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        // args list contains a real null — distinct from the string "null"
        List<String> args = new java.util.ArrayList<>();
        args.add(null);
        args.add("explicit");
        AttributedInvocation attrInv = new AttributedInvocation(args, List.of("testBar"));
        ClassTestCoverage attribution = new ClassTestCoverage("com/example/Foo",
                Map.of(0, List.of(attrInv)));
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(), Map.of(), List.of(), attribution);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        List<AttributedInvocation> invocations = result.classes().get("com/example/Foo")
                .testAttribution().probeInvocations().get(0);
        assertThat(invocations).hasSize(1);
        assertThat(invocations.get(0).args().get(0)).isNull();
        assertThat(invocations.get(0).args().get(1)).isEqualTo("explicit");
    }

    @Test
    void testAttribution_noAttribution_roundTrip(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc)));

        ExecutionData result = coverageDataReader.read(file);
        ClassTestCoverage readAttr = result.classes().get("com/example/Foo").testAttribution();
        assertThat(readAttr.probeInvocations()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Multiple classes
    // -------------------------------------------------------------------------

    @Test
    void multipleClasses_orderPreserved_noDataBleed(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test.exec");

        ClassCoverage cc1 = new ClassCoverage("com/example/Alpha", new boolean[]{true, false}, Map.of(), Map.of(), List.of(), null);
        ClassCoverage cc2 = new ClassCoverage("com/example/Beta", new boolean[]{false, false, true}, Map.of(), Map.of(), List.of(), null);
        coverageDataWriter.write(file, new ExecutionData(List.of(cc1, cc2)));

        ExecutionData result = coverageDataReader.read(file);
        assertThat(result.classCount()).isEqualTo(2);

        ClassCoverage readAlpha = result.classes().get("com/example/Alpha");
        assertThat(readAlpha.probeHits()).containsExactly(true, false);

        ClassCoverage readBeta = result.classes().get("com/example/Beta");
        assertThat(readBeta.probeHits()).containsExactly(false, false, true);

        assertThat(readAlpha.methodHits()).isEmpty();
        assertThat(readBeta.methodHits()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    void unknownProbeTag_throwsIOException(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("bad-tag.exec");

        // Write a file with one class and one probe entry that has an unknown tag
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            dos.writeInt(1); // classCount
            dos.writeUTF("com/example/Bad"); // classId
            dos.writeInt(1); // probeHits count
            dos.writeBoolean(false); // probeHit[0]
            dos.writeInt(1); // probeMetadata count
            dos.writeInt(0); // probeId
            dos.writeUTF("UNKNOWN_TAG"); // unknown tag
            dos.writeUTF("someMethod"); // methodName
            // no type-specific fields — reader should fail on tag lookup
            dos.flush();
        }

        assertThatThrownBy(() -> coverageDataReader.read(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("UNKNOWN_TAG");
    }

    // -------------------------------------------------------------------------
    // Immutability contracts
    // -------------------------------------------------------------------------

    @Test
    void classCoverage_mutatingConstructorArray_doesNotAffectStored() {
        boolean[] original = {true, false, true};
        ClassCoverage cc = new ClassCoverage("com/example/Foo", original, Map.of(), Map.of(), List.of(), null);

        original[0] = false;

        assertThat(cc.probeHits()[0]).isTrue();
    }

    @Test
    void classCoverage_mutatingAccessorArray_doesNotAffectSubsequentCall() {
        ClassCoverage cc = new ClassCoverage("com/example/Foo", new boolean[]{true, false}, Map.of(), Map.of(), List.of(), null);

        boolean[] firstCall = cc.probeHits();
        firstCall[0] = false;

        assertThat(cc.probeHits()[0]).isTrue();
    }
}
