package io.github.kd656.coveragex.core.collect;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MethodArgumentCaptureTest {

    @Test
    void nullArgElement_isPreservedAsNull() {
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{null});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNull()).isTrue();
        assertThat(result.get(0).value()).isEqualTo("null");
    }

    @Test
    void arrayArg_usesDefaultToStringRepresentation() {
        int[] arr = {1, 2, 3};
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{arr});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNull()).isFalse();
        // int[] uses Object.toString() by default — result is "[I@<hash>" (non-deterministic)
        // document the behavior: value is non-null and starts with the array type prefix
        assertThat(result.get(0).value()).startsWith("[I@");
    }

    @Test
    void collectionArg_usesIterableToStringRepresentation() {
        List<String> list = List.of("alpha", "beta");
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{list});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNull()).isFalse();
        assertThat(result.get(0).value()).isEqualTo("[alpha, beta]");
    }

    @Test
    void objectWithCustomToString_usesCustomRepresentation() {
        Object custom = new Object() {
            @Override
            public String toString() {
                return "domain:42";
            }
        };
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{custom});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).isEqualTo("domain:42");
    }

    @Test
    void objectWithThrowingToString_collectorDoesNotFail() {
        Object throwing = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        };
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{throwing});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNull()).isFalse();
        assertThat(result.get(0).value()).isEqualTo("toString failure");
    }

    @Test
    void veryLongArgString_isTruncatedAt256Characters() {
        String long500 = "x".repeat(500);
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{long500});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).value()).hasSize(257); // 256 chars + "…"
        assertThat(result.get(0).value()).endsWith("…");
    }

    @Test
    void cyclicObjectGraph_doesNotOverflow() {
        Object[] box = new Object[1];
        box[0] = new Object() {
            @Override
            public String toString() {
                return "cyclic:" + box[0].toString();
            }
        };
        List<SerializedArg> result = MethodArgumentCapture.capture(new Object[]{box[0]});
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNull()).isFalse();
        assertThat(result.get(0).value()).isEqualTo("Error occurred while serializing arguments in StackOverflowError");
    }
}
