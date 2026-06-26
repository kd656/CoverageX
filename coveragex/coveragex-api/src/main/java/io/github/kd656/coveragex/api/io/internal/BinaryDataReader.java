package io.github.kd656.coveragex.api.io.internal;

import io.github.kd656.coveragex.api.data.AttributedInvocation;
import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.MethodHit;
import io.github.kd656.coveragex.api.data.OperandKind;
import io.github.kd656.coveragex.api.data.ProbeHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.io.CoverageDataReader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class BinaryDataReader implements CoverageDataReader<ExecutionData> {

    @Override
    public ExecutionData read(Path path) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {

            int classCount = dis.readInt();
            List<ClassCoverage> classes = new ArrayList<>(classCount);

            for (int i = 0; i < classCount; i++) {
                classes.add(readClass(dis));
            }
            skipContextSection(dis);

            return new ExecutionData(classes);
        }
    }

    private ClassCoverage readClass(DataInputStream dis) throws IOException {
        String classId                     = dis.readUTF();
        boolean[] probeHits                = readProbeHits(dis);
        List<ProbeMetadata> metadata       = readProbeMetadata(dis);
        Map<Integer, MethodHit> methodHits = readMethodHits(dis);
        Map<Integer, ProbeHit> hits        = readHits(dis);
        ClassTestCoverage attribution      = readProbeAttribution(dis, classId);

        return new ClassCoverage(classId, probeHits, methodHits, hits, metadata, attribution);
    }

    private Map<Integer, ProbeHit> readHits(DataInputStream dis) throws IOException {
        int count = dis.readInt();
        if (count == 0) {
            return Collections.emptyMap();
        }

        Map<Integer, ProbeHit> result = new HashMap<>();
        for (int i = 0; i < count; i++) {
            int probeId = dis.readInt();
            int hitCount = dis.readInt();
            result.put(probeId, new ProbeHit(probeId, hitCount));
        }

        return result;
    }

    private boolean[] readProbeHits(DataInputStream dis) throws IOException {
        int count = dis.readInt();

        boolean[] hits = new boolean[count];
        for (int i = 0; i < count; i++) {
            hits[i] = dis.readBoolean();
        }

        return hits;
    }

    private List<ProbeMetadata> readProbeMetadata(DataInputStream dis) throws IOException {
        int count = dis.readInt();

        List<ProbeMetadata> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(readOneProbeMetadata(dis));
        }

        return entries;
    }

    private ProbeMetadata readOneProbeMetadata(DataInputStream dis) throws IOException {
        int probeId       = dis.readInt();
        String tag        = dis.readUTF();
        String methodName = dis.readUTF();
        return switch (tag) {
            case ProbeMetadata.MethodProbe.TAG  -> readMethodProbe(dis, probeId, methodName);
            case ProbeMetadata.BranchProbe.TAG  -> readBranchProbe(dis, probeId, methodName);
            case ProbeMetadata.ReturnProbe.TAG  -> new ProbeMetadata.ReturnProbe(probeId, methodName, dis.readInt());
            case ProbeMetadata.ThrowProbe.TAG   -> new ProbeMetadata.ThrowProbe (probeId, methodName, dis.readInt());
            case ProbeMetadata.SegmentProbe.TAG -> new ProbeMetadata.SegmentProbe(probeId, methodName, dis.readInt(), dis.readInt());
            default -> throw new IOException("Unknown probe type tag: " + tag);
        };
    }

    /**
     * Reads a {@link ProbeMetadata.MethodProbe} from the stream.
     *
     * <p>Field order mirrors {@link ProbeMetadataSerializer#visit(ProbeMetadata.MethodProbe)}:</p>
     * <ol>
     *   <li>{@code int}    — start line</li>
     *   <li>{@code int}    — end line</li>
     *   <li>{@code int}    — parameter name count</li>
     *   <li>{@code UTF}×N — parameter names</li>
     * </ol>
     *
     * @param dis        the input stream positioned immediately after the common header
     * @param probeId    the probe id already read from the common header
     * @param methodName the method name already read from the common header
     * @return the fully populated {@link ProbeMetadata.MethodProbe}
     * @throws IOException if the stream cannot be read
     */
    private ProbeMetadata.MethodProbe readMethodProbe(DataInputStream dis,
                                                      int probeId,
                                                      String methodName) throws IOException {
        int startLine = dis.readInt();
        int endLine = dis.readInt();
        int nameCount = dis.readInt();
        List<String> parameterNames = new ArrayList<>(nameCount);
        for (int i = 0; i < nameCount; i++) {
            parameterNames.add(dis.readUTF());
        }
        return new ProbeMetadata.MethodProbe(probeId, methodName, startLine, endLine, parameterNames);
    }

    /**
     * Reads a {@link ProbeMetadata.BranchProbe} from the stream.
     *
     * <p>Field order mirrors {@link ProbeMetadataSerializer#visit(ProbeMetadata.BranchProbe)}:</p>
     * <ol>
     *   <li>{@code int}    — source line</li>
     *   <li>{@code UTF}    — condition text</li>
     *   <li>{@code UTF}    — direction name</li>
     *   <li>{@code int}    — condition id</li>
     *   <li>{@code int}    — operand kind stable code (see {@link OperandKind#code()})</li>
     *   <li>{@code int}    — arg label count</li>
     *   <li>{@code UTF}×N — arg labels</li>
     * </ol>
     *
     * @param dis        the input stream positioned immediately after the common header
     * @param probeId    the probe id already read from the common header
     * @param methodName the method name already read from the common header
     * @return the fully populated {@link ProbeMetadata.BranchProbe}
     * @throws IOException if the stream cannot be read
     */
    private ProbeMetadata.BranchProbe readBranchProbe(DataInputStream dis,
                                                       int probeId,
                                                       String methodName) throws IOException {
        int line = dis.readInt();
        String conditionText = dis.readUTF();
        ProbeMetadata.BranchDirection direction = ProbeMetadata.BranchDirection.valueOf(dis.readUTF());
        int conditionId = dis.readInt();
        int kindCode = dis.readInt();
        OperandKind kind;
        try {
            kind = OperandKind.fromCode(kindCode);
        } catch (IllegalArgumentException e) {
            throw new IOException("Unknown OperandKind code: " + kindCode, e);
        }
        int labelCount = dis.readInt();
        List<String> argLabels = new ArrayList<>(labelCount);
        for (int i = 0; i < labelCount; i++) {
            argLabels.add(dis.readUTF());
        }
        return new ProbeMetadata.BranchProbe(probeId, methodName, line, conditionText,
                direction, conditionId, kind, argLabels);
    }

    private Map<Integer, MethodHit> readMethodHits(DataInputStream dis) throws IOException {
        int count = dis.readInt();

        if (count == 0) {
            return Collections.emptyMap();
        }

        Map<Integer, MethodHit> result = new HashMap<>(count * 2);

        for (int i = 0; i < count; i++) {
            int probeId = dis.readInt();
            String methodName = dis.readUTF();
            int invCount = dis.readInt();

            List<InvocationRecord> invocations = new ArrayList<>(invCount);
            for (int j = 0; j < invCount; j++) {
                int argCount = dis.readInt();

                List<String> args = new ArrayList<>(argCount);
                for (int k = 0; k < argCount; k++) {
                    args.add(BinaryFormatUtil.readNullableString(dis));
                }
                invocations.add(new InvocationRecord(args, dis.readInt()));
            }

            result.put(probeId, new MethodHit(methodName, invocations));
        }

        return result;
    }

    private ClassTestCoverage readProbeAttribution(DataInputStream dis, String classId) throws IOException {
        int probeCount = dis.readInt();
        if (probeCount == 0) return ClassTestCoverage.empty(classId);
        Map<Integer, List<AttributedInvocation>> probeInvocations = new HashMap<>(probeCount * 2);
        for (int i = 0; i < probeCount; i++) {
            int probeId = dis.readInt();
            int invCount = dis.readInt();
            List<AttributedInvocation> invocations = new ArrayList<>(invCount);
            for (int j = 0; j < invCount; j++) {
                int argCount = dis.readInt();
                List<String> args = new ArrayList<>(argCount);
                for (int k = 0; k < argCount; k++) {
                    args.add(BinaryFormatUtil.readNullableString(dis));
                }

                int testCount = dis.readInt();
                List<String> tests = new ArrayList<>(testCount);
                for (int k = 0; k < testCount; k++) {
                    tests.add(dis.readUTF());
                }

                invocations.add(new AttributedInvocation(args, tests));
            }
            probeInvocations.put(probeId, List.copyOf(invocations));
        }
        return new ClassTestCoverage(classId, Map.copyOf(probeInvocations));
    }

    private void skipContextSection(DataInputStream dis) throws IOException {
        dis.readInt(); // contextCount — always 0, reserved
    }
}
