package com.coveragex.api.io.internal;

import com.coveragex.api.data.AttributedInvocation;
import com.coveragex.api.data.ClassCoverage;
import com.coveragex.api.data.ClassTestCoverage;
import com.coveragex.api.data.ExecutionData;
import com.coveragex.api.data.InvocationRecord;
import com.coveragex.api.data.MethodHit;
import com.coveragex.api.data.ProbeHit;
import com.coveragex.api.data.ProbeMetadata;
import com.coveragex.api.io.CoverageDataReader;

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
            case ProbeMetadata.MethodProbe.TAG  -> new ProbeMetadata.MethodProbe(probeId, methodName, dis.readInt(), dis.readInt());
            case ProbeMetadata.BranchProbe.TAG  -> new ProbeMetadata.BranchProbe(probeId, methodName, dis.readInt(),
                                                        dis.readUTF(), ProbeMetadata.BranchDirection.valueOf(dis.readUTF()));
            case ProbeMetadata.ReturnProbe.TAG  -> new ProbeMetadata.ReturnProbe(probeId, methodName, dis.readInt());
            case ProbeMetadata.ThrowProbe.TAG   -> new ProbeMetadata.ThrowProbe (probeId, methodName, dis.readInt());
            case ProbeMetadata.SegmentProbe.TAG -> new ProbeMetadata.SegmentProbe(probeId, methodName, dis.readInt(), dis.readInt());
            default -> throw new IOException("Unknown probe type tag: " + tag);
        };
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
