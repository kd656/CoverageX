package io.github.kd656.coveragex.core.report.views.html.payload;

import com.alibaba.fastjson2.annotation.JSONField;
import java.util.List;

/**
 * Compact JSON payload for one unique argument combination seen during test execution.
 * {@code testMethods} lists the test method names that produced these args.
 * {@code args} elements are String (or null for Java null arguments).
 */
public record InvocationPayload(
    @JSONField(name = "ts")  List<String> testMethods,
    @JSONField(name = "a")   List<String> args,
    @JSONField(name = "cnt") int count
) {}
