package io.github.kd656.coveragex.core.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MethodNameFormatter}, covering every input shape
 * defined in the class-level mapping table.
 */
class MethodNameFormatterTest {

    @Test
    void constructorMapsToReadableName() {
        assertThat(MethodNameFormatter.format("<init>")).isEqualTo("Constructor");
    }

    @Test
    void staticInitialiserMapsToReadableName() {
        assertThat(MethodNameFormatter.format("<clinit>")).isEqualTo("Static initialiser");
    }

    @Test
    void lambdaIndexZeroShiftedToOne() {
        assertThat(MethodNameFormatter.format("lambda$classify$0"))
                .isEqualTo("classify (lambda #1)");
    }

    @Test
    void lambdaIndexOneShiftedToTwo() {
        assertThat(MethodNameFormatter.format("lambda$classify$1"))
                .isEqualTo("classify (lambda #2)");
    }

    @Test
    void lambdaWithDifferentEnclosingMethod() {
        assertThat(MethodNameFormatter.format("lambda$process$0"))
                .isEqualTo("process (lambda #1)");
    }

    @Test
    void lambdaWithHighIndex() {
        assertThat(MethodNameFormatter.format("lambda$foo$9"))
                .isEqualTo("foo (lambda #10)");
    }

    @Test
    void accessorZeroMapsToPredefinedLabel() {
        assertThat(MethodNameFormatter.format("access$0"))
                .isEqualTo("synthetic accessor");
    }

    @Test
    void accessorHundredMapsToPredefinedLabel() {
        assertThat(MethodNameFormatter.format("access$100"))
                .isEqualTo("synthetic accessor");
    }

    @Test
    void ordinaryNameIsPassedThrough() {
        assertThat(MethodNameFormatter.format("compute")).isEqualTo("compute");
    }

    @Test
    void getNameIsPassedThrough() {
        assertThat(MethodNameFormatter.format("getName")).isEqualTo("getName");
    }

    @Test
    void toStringIsPassedThrough() {
        assertThat(MethodNameFormatter.format("toString")).isEqualTo("toString");
    }

    @Test
    void equalsIsPassedThrough() {
        assertThat(MethodNameFormatter.format("equals")).isEqualTo("equals");
    }

    @Test
    void nullInputReturnsEmptyString() {
        assertThat(MethodNameFormatter.format(null)).isEqualTo("");
    }
}
