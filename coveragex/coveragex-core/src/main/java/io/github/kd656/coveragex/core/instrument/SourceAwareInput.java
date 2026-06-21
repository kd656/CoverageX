package io.github.kd656.coveragex.core.instrument;

import io.github.kd656.coveragex.core.analysis.source.model.ClassModel;

/**
 * Input wrapper for {@link ProbeInjector} implementations that require both the
 * raw class bytecode and the source-level {@link ClassModel} from the coverage map.
 *
 * <p>By encapsulating both pieces of data in a single typed parameter, the
 * {@code ProbeInjector<SourceAwareInput>} contract makes the requirement for
 * a pre-loaded source map explicit at the call site.</p>
 *
 * @param classBytes the original class file bytes to be instrumented
 * @param classModel the source map model for the class being instrumented;
 *                   used to look up decision models and populate
 *                   {@code BranchProbe.conditionText} with real source text
 */
public record SourceAwareInput(byte[] classBytes, ClassModel classModel) {}
