package io.github.kd656.coveragex.core.analysis.source;

import com.github.javaparser.JavaParser;
import io.github.kd656.coveragex.core.analysis.source.impl.SourceCodeAnalyzer;
import io.github.kd656.coveragex.core.analysis.source.model.MethodReference;
import io.github.kd656.coveragex.core.analysis.source.model.SemanticIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCodeAnalyzerRecordTest {

    @Test
    void topLevelRecordWithExplicitMethodIsSourceMapped(@TempDir Path srcRoot) throws IOException {
        Path pkg = Files.createDirectories(srcRoot.resolve("example/dto"));
        Files.writeString(pkg.resolve("UserRecord.java"), """
                package example.dto;

                public record UserRecord(String name, int age) {
                    public boolean adult() {
                        return age >= 18;
                    }
                }
                """);

        SemanticIndex index = analyze(srcRoot);

        var classModel = index.getClasses().get("example/dto/UserRecord");
        assertThat(classModel).isNotNull();
        assertThat(classModel.getSourceFile()).isEqualTo("example/dto/UserRecord.java");
        var method = classModel.getMethods().get(new MethodReference("adult", "()Z"));
        assertThat(method).isNotNull();
        assertThat(method.getDecisionsList()).hasSize(1);
        assertThat(method.getDecisionsList().getFirst().kind()).isEqualTo("RETURN");
        assertThat(method.getDecisionsList().getFirst().operands().getFirst().conditionText())
                .isEqualTo("age >= 18");
    }

    @Test
    void nestedRecordUsesJvmDollarNameAndContainingSourceFile(@TempDir Path srcRoot) throws IOException {
        Path pkg = Files.createDirectories(srcRoot.resolve("example/dto"));
        Files.writeString(pkg.resolve("UserTypes.java"), """
                package example.dto;

                public final class UserTypes {
                    public record UserRecord(String name, int age) {
                        public boolean adult() {
                            return age >= 18;
                        }
                    }
                }
                """);

        SemanticIndex index = analyze(srcRoot);

        assertThat(index.getClasses()).containsKey("example/dto/UserTypes$UserRecord");
        var nested = index.getClasses().get("example/dto/UserTypes$UserRecord");
        assertThat(nested.getSourceFile()).isEqualTo("example/dto/UserTypes.java");
        assertThat(nested.getMethods()).containsKey(new MethodReference("adult", "()Z"));
    }

    @Test
    void compactConstructorDecisionIsMapped(@TempDir Path srcRoot) throws IOException {
        Files.writeString(srcRoot.resolve("Positive.java"), """
                public record Positive(int value) {
                    public Positive {
                        if (value <= 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                }
                """);

        SemanticIndex index = analyze(srcRoot);

        var classModel = index.getClasses().get("Positive");
        assertThat(classModel).isNotNull();
        var constructor = classModel.getMethods().get(new MethodReference("<init>", "(I)V"));
        assertThat(constructor).isNotNull();
        assertThat(constructor.getParameterNames()).containsExactly("value");
        assertThat(constructor.getDecisionsList()).hasSize(1);
        assertThat(constructor.getDecisionsList().getFirst().kind()).isEqualTo("IF");
    }

    private static SemanticIndex analyze(Path srcRoot) throws IOException {
        SemanticIndex index = new SemanticIndex();
        new SourceCodeAnalyzer(new JavaParser(), index).scan(srcRoot);
        return index;
    }
}
