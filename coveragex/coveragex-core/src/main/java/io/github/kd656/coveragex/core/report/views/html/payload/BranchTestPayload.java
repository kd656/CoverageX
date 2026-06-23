package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * Compact JSON payload for one test that exercised a branch direction.
 *
 * <p>The {@code testClassName} and {@code argValues} fields are reserved
 * for later phases and are not populated yet:
 * <ul>
 *   <li>{@code testClassName} will hold the fully qualified test class
 *       name once the JUnit listener forwards it through
 *       {@link io.github.kd656.coveragex.api.context.ProbeExecutionContext};
 *       currently always {@code null}.</li>
 *   <li>{@code argValues} will hold the operand values captured at probe
 *       time once branch operand-value capture lands. The list will be
 *       positionally aligned with the parent
 *       {@link BranchConditionPayload#operandArgs} schema. Currently
 *       always empty.</li>
 * </ul>
 *
 * @param testMethodName the simple name of the test method
 * @param testClassName  reserved; always {@code null} today
 * @param count          the number of times this test hit the direction
 * @param argValues      reserved; always empty today
 */
public record BranchTestPayload(
        @JSONField(name = "t")    String testMethodName,
        @JSONField(name = "cls")  String testClassName,
        @JSONField(name = "cnt")  int count,
        @JSONField(name = "args") List<String> argValues) {
}
