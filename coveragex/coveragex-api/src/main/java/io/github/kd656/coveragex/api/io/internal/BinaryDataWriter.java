package io.github.kd656.coveragex.api.io.internal;

import io.github.kd656.coveragex.api.data.ClassCoverage;
import io.github.kd656.coveragex.api.data.ExecutionData;
import io.github.kd656.coveragex.api.data.InvocationRecord;
import io.github.kd656.coveragex.api.data.MethodHit;
import io.github.kd656.coveragex.api.data.ProbeHit;
import io.github.kd656.coveragex.api.data.ProbeMetadata;
import io.github.kd656.coveragex.api.data.AttributedInvocation;
import io.github.kd656.coveragex.api.data.ClassTestCoverage;
import io.github.kd656.coveragex.api.io.CoverageDataWriter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BinaryDataWriter implements CoverageDataWriter<ExecutionData> {

    @Override
    public void write(Path outputPath, ExecutionData data) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(outputPath)))) {

            dos.writeInt(data.classCount());
            for (ClassCoverage cc : data.classes().values()) {
                writeClass(dos, cc);
            }
            dos.writeInt(0); // context count — reserved
            dos.flush();
        }
    }

    private void writeClass(DataOutputStream dos, ClassCoverage cc) throws IOException {
        dos.writeUTF(cc.classId());
        writeProbeHits(dos, cc.probeHits());
        writeProbeMetadata(dos, cc.probeMetadata());
        writeMethodHits(dos, cc.methodHits());
        writeHits(dos, cc.hits());
        writeProbeAttribution(dos, cc.testAttribution());
    }

    private void writeHits(DataOutputStream dos, Map<Integer, ProbeHit> hits) throws IOException {
        dos.writeInt(hits.size());
        for (Map.Entry<Integer, ProbeHit> e : new TreeMap<>(hits).entrySet()) {
            dos.writeInt(e.getKey());
            dos.writeInt(e.getValue().count());
        }
    }

    private void writeProbeHits(DataOutputStream dos, boolean[] hits) throws IOException {
        dos.writeInt(hits.length);
        for (boolean hit : hits) dos.writeBoolean(hit);
    }

    private void writeProbeMetadata(DataOutputStream dos, List<ProbeMetadata> metadata) throws IOException {
        dos.writeInt(metadata.size());
        ProbeMetadataSerializer serializer = new ProbeMetadataSerializer(dos);
        for (ProbeMetadata meta : metadata) {
            dos.writeInt(meta.probeId());
            dos.writeUTF(meta.tag());
            dos.writeUTF(meta.methodName());
            meta.accept(serializer);
        }
    }

    private void writeMethodHits(DataOutputStream dos, Map<Integer, MethodHit> methodHits) throws IOException {
        dos.writeInt(methodHits.size());
        for (Map.Entry<Integer, MethodHit> e : new TreeMap<>(methodHits).entrySet()) {
            dos.writeInt(e.getKey());
            dos.writeUTF(e.getValue().methodName());
            dos.writeInt(e.getValue().invocations().size());
            for (InvocationRecord inv : e.getValue().invocations()) {
                dos.writeInt(inv.args().size());
                for (String arg : inv.args()) BinaryFormatUtil.writeNullableString(dos, arg);
                dos.writeInt(inv.count());
            }
        }
    }

    private void writeProbeAttribution(DataOutputStream dos, ClassTestCoverage attribution) throws IOException {
        Map<Integer, List<AttributedInvocation>> probeInvocations = attribution.probeInvocations();
        dos.writeInt(probeInvocations.size());
        for (Map.Entry<Integer, List<AttributedInvocation>> e : new TreeMap<>(probeInvocations).entrySet()) {
            dos.writeInt(e.getKey());
            dos.writeInt(e.getValue().size());
            for (AttributedInvocation inv : e.getValue()) {
                dos.writeInt(inv.args().size());
                for (String arg : inv.args()) BinaryFormatUtil.writeNullableString(dos, arg);
                dos.writeInt(inv.testMethods().size());
                for (String test : inv.testMethods()) dos.writeUTF(test);
            }
        }
    }
}
