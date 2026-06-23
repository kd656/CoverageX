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
                11,
                List.of("name", "count")
        );

        String json = mapper.writeValueAsString(constructor);

        assertThat(json).contains("\"isConstructor\":true");
        assertThat(json).doesNotContain("\"constructor\"");

        MethodModel restored = mapper.readValue(json, MethodModel.class);
        assertThat(restored.isConstructor()).isTrue();
        assertThat(restored.getName()).isEqualTo("<init>");
        assertThat(restored.getParameterNames()).containsExactly("name", "count");
    }

    @Test
    void parameterNames_roundTripThroughJson() throws Exception {
        MethodModel method = MethodModel.createMethod(
                "org/example/Util",
                "classify",
                "(Ljava/lang/String;I)Ljava/lang/String;",
                2,
                List.of("java.lang.String", "int"),
                "String",
                true,
                true,
                false,
                false,
                false,
                10,
                15,
                List.of("name", "threshold")
        );

        String json = mapper.writeValueAsString(method);

        assertThat(json).contains("\"parameterNames\"");

        MethodModel restored = mapper.readValue(json, MethodModel.class);
        assertThat(restored.getParameterNames()).containsExactly("name", "threshold");
    }

    @Test
    void parameterNames_absentInJson_defaultsToEmpty() throws Exception {
        // Simulates reading an older map file that has no parameterNames field.
        String json = "{\"className\":\"org/example/Foo\",\"name\":\"run\","
                + "\"descriptor\":\"()V\",\"parametersCount\":0,"
                + "\"parametersTypes\":[],\"returnType\":\"void\","
                + "\"static\":false,\"public\":true,\"private\":false,"
                + "\"protected\":false,\"abstract\":false,"
                + "\"startLine\":5,\"endLine\":10,\"isConstructor\":false}";

        MethodModel restored = mapper.readValue(json, MethodModel.class);
        assertThat(restored.getParameterNames()).isEmpty();
    }
}
