package io.github.kd656.coveragex.core.analysis.source;

import io.github.kd656.coveragex.core.analysis.source.model.MethodModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MethodModelSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void constructorFlagRoundTripsWithStableJsonPropertyName() throws Exception {
        MethodModel constructor = MethodModel.createConstructor(
                "org/example/SimpleObject",
                "(Ljava/lang/String;I)V",
                2,
                List.of("java.lang.String", "int"),
                true,
                false,
                false,
                8,
                11
        );

        String json = mapper.writeValueAsString(constructor);

        assertThat(json).contains("\"isConstructor\":true");
        assertThat(json).doesNotContain("\"constructor\"");

        MethodModel restored = mapper.readValue(json, MethodModel.class);
        assertThat(restored.isConstructor()).isTrue();
        assertThat(restored.getName()).isEqualTo("<init>");
    }
}
