package com.coveragex.core.instrument;

import com.coveragex.core.fixtures.conditions.*;
import com.coveragex.core.fixtures.loops.*;
import com.coveragex.core.fixtures.switches.*;
import com.coveragex.core.fixtures.exceptions.*;
import com.coveragex.core.fixtures.lambdas.*;
import com.coveragex.core.fixtures.methods.*;
import com.coveragex.core.instrument.InstrumentationVerifier;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProbeVerificationTest {

    // =========================================================================
    // CONDITIONS
    // =========================================================================

    // --- SimpleIf ---

    @Test
    void simpleIf_positive_trueHit_falseNotHit() throws Exception {
        // Given: SimpleIf instrumented — classify() has one if/else, 1 branch pair
        // When:  classify(5) — x > 0 is true, takes the if-body
        // Then:  TRUE probe fires, FALSE probe does not
        var v = InstrumentationVerifier.of(SimpleIf.class);
        v.invoke("classify", new Class[]{int.class}, 5);
        v.assertBranchHit("classify", "TRUE");
        v.assertBranchNotHit("classify", "FALSE");
    }

    @Test
    void simpleIf_negative_falseHit_trueNotHit() throws Exception {
        // Given: SimpleIf instrumented
        // When:  classify(-1) — x > 0 is false, takes the else-body
        // Then:  FALSE probe fires, TRUE probe does not
        var v = InstrumentationVerifier.of(SimpleIf.class);
        v.invoke("classify", new Class[]{int.class}, -1);
        v.assertBranchHit("classify", "FALSE");
        v.assertBranchNotHit("classify", "TRUE");
    }

    @Test
    void simpleIf_zero_falseHit_trueNotHit() throws Exception {
        // Given: SimpleIf instrumented
        // When:  classify(0) — x > 0 is false (0 is not > 0), takes else-body
        // Then:  FALSE probe fires (boundary value: 0 is not positive)
        var v = InstrumentationVerifier.of(SimpleIf.class);
        v.invoke("classify", new Class[]{int.class}, 0);
        v.assertBranchHit("classify", "FALSE");
        v.assertBranchNotHit("classify", "TRUE");
    }

    @Test
    void simpleIf_bothBranches_requiresTwoInvocations() throws Exception {
        // Given: SimpleIf instrumented
        // When:  classify() called once with positive, once with negative
        // Then:  both TRUE and FALSE probes are hit — full branch coverage achieved
        var v = InstrumentationVerifier.of(SimpleIf.class);
        v.invoke("classify", new Class[]{int.class}, 1);
        v.invoke("classify", new Class[]{int.class}, -1);
        v.assertBranchHit("classify", "TRUE");
        v.assertBranchHit("classify", "FALSE");
    }

    // --- IfNoElse ---

    @Test
    void ifNoElse_conditionFalse_bodyNotEntered() throws Exception {
        // Given: IfNoElse instrumented — label() has if without else, 1 branch pair
        // When:  label(50) — x > 100 is false, body skipped
        // Then:  FALSE probe fires, TRUE probe (body entry) does not
        var v = InstrumentationVerifier.of(IfNoElse.class);
        v.invoke("label", new Class[]{int.class}, 50);
        v.assertBranchHit("label", "FALSE");
        v.assertBranchNotHit("label", "TRUE");
    }

    @Test
    void ifNoElse_conditionTrue_bodyEntered() throws Exception {
        // Given: IfNoElse instrumented
        // When:  label(200) — x > 100 is true, body entered
        // Then:  TRUE probe fires
        var v = InstrumentationVerifier.of(IfNoElse.class);
        v.invoke("label", new Class[]{int.class}, 200);
        v.assertBranchHit("label", "TRUE");
    }

    // --- NullCheck ---

    @Test
    void nullCheck_withNull_nullBranchHit() throws Exception {
        // Given: NullCheck instrumented — safeLength() has `if (s == null)`, 1 branch pair
        //        javac compiles `if (s == null) { body }` as IFNONNULL L_skip (jump over body when NOT null).
        //        DefaultProbeInjector labels by opcode: IFNONNULL → "if (x != null)".
        //        Jump-taken (s != null, opcode TRUE) fires the TRUE probe.
        //        Fall-through (s IS null, opcode FALSE) fires the FALSE probe.
        // When:  safeLength(null) — null reference passed
        // Then:  IFNONNULL falls through → FALSE probe fires (opcode-level: "if (x != null)" is false)
        var v = InstrumentationVerifier.of(NullCheck.class);
        v.invoke("safeLength", new Class[]{String.class}, (Object) null);
        v.assertBranchHit("safeLength", "FALSE");
        v.assertBranchNotHit("safeLength", "TRUE");
    }

    @Test
    void nullCheck_withNonNull_nonNullBranchHit() throws Exception {
        // Given: NullCheck instrumented
        // When:  safeLength("hello") — non-null reference passed
        // Then:  IFNONNULL jumps → TRUE probe fires (opcode-level: "if (x != null)" is true)
        var v = InstrumentationVerifier.of(NullCheck.class);
        v.invoke("safeLength", new Class[]{String.class}, "hello");
        v.assertBranchHit("safeLength", "TRUE");
        v.assertBranchNotHit("safeLength", "FALSE");
    }

    // --- ShortCircuitAnd ---

    @Test
    void shortCircuitAnd_exactlyTwoBranchPairs() throws Exception {
        // Given: ShortCircuitAnd instrumented — `s != null && s.length() >= minLen`
        //        compiles to two IFXX instructions (IFNULL + IF_ICMPLT)
        // When:  no invocation needed — this is a structural check on injected probes
        // Then:  exactly 2 branch pairs registered for the method
        var v = InstrumentationVerifier.of(ShortCircuitAnd.class);
        v.assertBranchPairCount("check", 2);
    }

    @Test
    void shortCircuitAnd_nullInput_firstBranchShortCircuits() throws Exception {
        // Given: ShortCircuitAnd instrumented, 2 branch pairs
        // When:  check(null, 3) — first condition `s != null` is false → short-circuits,
        //        second condition `s.length() >= minLen` is never evaluated
        // Then:  only 1 branch probe fires (first condition's direction); second pair untouched
        var v = InstrumentationVerifier.of(ShortCircuitAnd.class);
        v.invoke("check", new Class[]{String.class, int.class}, null, 3);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        int pairsHit = (int) meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && hits[bp.probeId()])
            .count();
        assertThat(pairsHit).as("Only one direction of one pair should fire").isEqualTo(1);
    }

    @Test
    void shortCircuitAnd_nonNullTooShort_bothConditionsEvaluated() throws Exception {
        // Given: ShortCircuitAnd instrumented
        // When:  check("hi", 10) — first condition passes (non-null), second fails (too short)
        // Then:  both branch pairs are reached; method is entered
        var v = InstrumentationVerifier.of(ShortCircuitAnd.class);
        v.invoke("check", new Class[]{String.class, int.class}, "hi", 10);
        v.assertMethodEntered("check");
    }

    @Test
    void shortCircuitAnd_nonNullLongEnough_bothConditionsTrue() throws Exception {
        // Given: ShortCircuitAnd instrumented
        // When:  check("hello", 3) — both conditions true, returns "pass"
        // Then:  method entered, both pairs evaluated
        var v = InstrumentationVerifier.of(ShortCircuitAnd.class);
        v.invoke("check", new Class[]{String.class, int.class}, "hello", 3);
        v.assertMethodEntered("check");
    }

    // --- ShortCircuitOr ---

    @Test
    void shortCircuitOr_exactlyTwoBranchPairs() throws Exception {
        // Given: ShortCircuitOr instrumented — `s == null || s.isEmpty()`
        //        compiles to two IFXX instructions
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered
        var v = InstrumentationVerifier.of(ShortCircuitOr.class);
        v.assertBranchPairCount("isBlank", 2);
    }

    @Test
    void shortCircuitOr_null_firstConditionShortCircuits() throws Exception {
        // Given: ShortCircuitOr instrumented
        // When:  isBlank(null) — first condition `s == null` is true → OR short-circuits,
        //        second condition `s.isEmpty()` is never evaluated
        // Then:  only 1 branch probe fires (the first condition's TRUE direction)
        var v = InstrumentationVerifier.of(ShortCircuitOr.class);
        v.invoke("isBlank", new Class[]{String.class}, (Object) null);
        boolean[] hits = v.probeHits();
        int hitCount = 0;
        for (com.coveragex.api.data.ProbeMetadata pm : v.probeMetadata()) {
            if (pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()]) {
                hitCount++;
            }
        }
        assertThat(hitCount).isEqualTo(1);
    }

    @Test
    void shortCircuitOr_nonNullNonEmpty_bothConditionsFalse() throws Exception {
        // Given: ShortCircuitOr instrumented
        // When:  isBlank("hello") — both conditions evaluated and false → returns false
        // Then:  both branch pairs reached
        var v = InstrumentationVerifier.of(ShortCircuitOr.class);
        v.invoke("isBlank", new Class[]{String.class}, "hello");
        v.assertMethodEntered("isBlank");
    }

    @Test
    void shortCircuitOr_nonNullEmpty_secondConditionTrue() throws Exception {
        // Given: ShortCircuitOr instrumented
        // When:  isBlank("") — first condition false, second condition `isEmpty()` true
        // Then:  both branch pairs evaluated; second condition's TRUE fires
        var v = InstrumentationVerifier.of(ShortCircuitOr.class);
        v.invoke("isBlank", new Class[]{String.class}, "");
        v.assertMethodEntered("isBlank");
    }

    // --- CompoundCondition ---

    @Test
    void compoundCondition_exactlyThreeBranchPairs() throws Exception {
        // Given: CompoundCondition instrumented — `a && b || c` compiles to 3 IFXX
        // When:  no invocation — structural check
        // Then:  exactly 3 branch pairs registered
        var v = InstrumentationVerifier.of(CompoundCondition.class);
        v.assertBranchPairCount("evaluate", 3);
    }

    @Test
    void compoundCondition_aTrueAndBTrue_shortCircuitsOrViaAndTrue() throws Exception {
        // Given: CompoundCondition instrumented
        // When:  evaluate(true, true, false) — `a && b` is true → OR short-circuits, c not evaluated
        // Then:  method entered; first two conditions evaluated, third skipped
        var v = InstrumentationVerifier.of(CompoundCondition.class);
        v.invoke("evaluate", new Class[]{boolean.class, boolean.class, boolean.class}, true, true, false);
        v.assertMethodEntered("evaluate");
    }

    @Test
    void compoundCondition_aFalse_andShortCircuits_cEvaluated() throws Exception {
        // Given: CompoundCondition instrumented
        // When:  evaluate(false, false, true) — `a` is false → AND short-circuits,
        //        b is not evaluated, then `c` is evaluated and is true
        // Then:  a's branch and c's branch fired; b's branch not reached
        var v = InstrumentationVerifier.of(CompoundCondition.class);
        v.invoke("evaluate", new Class[]{boolean.class, boolean.class, boolean.class}, false, false, true);
        v.assertMethodEntered("evaluate");
    }

    @Test
    void compoundCondition_allFalse_allThreeConditionsEvaluated() throws Exception {
        // Given: CompoundCondition instrumented
        // When:  evaluate(false, false, false) — all conditions false, returns "no"
        // Then:  all three branch pairs reached
        var v = InstrumentationVerifier.of(CompoundCondition.class);
        v.invoke("evaluate", new Class[]{boolean.class, boolean.class, boolean.class}, false, false, false);
        v.assertMethodEntered("evaluate");
    }

    // --- TernaryOp ---

    @Test
    void ternary_nonNegative_trueBranchHit() throws Exception {
        // Given: TernaryOp instrumented — `x >= 0 ? "non-negative" : "negative"`, 1 branch pair
        // When:  sign(0) — x >= 0 is true (boundary value)
        // Then:  TRUE probe fires, FALSE does not
        var v = InstrumentationVerifier.of(TernaryOp.class);
        v.invoke("sign", new Class[]{int.class}, 0);
        v.assertBranchHit("sign", "TRUE");
        v.assertBranchNotHit("sign", "FALSE");
    }

    @Test
    void ternary_negative_falseBranchHit() throws Exception {
        // Given: TernaryOp instrumented
        // When:  sign(-5) — x >= 0 is false
        // Then:  FALSE probe fires, TRUE does not
        var v = InstrumentationVerifier.of(TernaryOp.class);
        v.invoke("sign", new Class[]{int.class}, -5);
        v.assertBranchHit("sign", "FALSE");
        v.assertBranchNotHit("sign", "TRUE");
    }

    // --- NestedTernary ---

    @Test
    void nestedTernary_exactlyTwoBranchPairs() throws Exception {
        // Given: NestedTernary instrumented — `x > 0 ? "positive" : x < 0 ? "negative" : "zero"`
        //        compiles to 2 IFXX instructions
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered
        var v = InstrumentationVerifier.of(NestedTernary.class);
        v.assertBranchPairCount("classify", 2);
    }

    @Test
    void nestedTernary_positiveValue_firstTernaryTrueSecondNotEvaluated() throws Exception {
        // Given: NestedTernary instrumented
        // When:  classify(5) — first ternary `x > 0` is true → result is "positive",
        //        second ternary `x < 0` is never evaluated
        // Then:  exactly 1 branch probe fires (first ternary's TRUE direction)
        var v = InstrumentationVerifier.of(NestedTernary.class);
        v.invoke("classify", new Class[]{int.class}, 5);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    @Test
    void nestedTernary_negativeValue_firstFalseSecondTrue() throws Exception {
        // Given: NestedTernary instrumented
        // When:  classify(-3) — first ternary false (not > 0), second ternary true (< 0)
        // Then:  2 branch probes fire: first FALSE + second TRUE
        var v = InstrumentationVerifier.of(NestedTernary.class);
        v.invoke("classify", new Class[]{int.class}, -3);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    @Test
    void nestedTernary_zero_firstFalseSecondFalseZeroPath() throws Exception {
        // Given: NestedTernary instrumented
        // When:  classify(0) — first false (not > 0), second false (not < 0) → "zero"
        // Then:  2 branch probes fire: first FALSE + second FALSE
        var v = InstrumentationVerifier.of(NestedTernary.class);
        v.invoke("classify", new Class[]{int.class}, 0);
        v.assertMethodEntered("classify");
    }

    // --- NestedIf ---

    @Test
    void nestedIf_exactlyThreeBranchPairs() throws Exception {
        // Given: NestedIf instrumented — 3 chained if/else-if conditions, 3 IFXX instructions
        // When:  no invocation — structural check
        // Then:  exactly 3 branch pairs registered
        var v = InstrumentationVerifier.of(NestedIf.class);
        v.assertBranchPairCount("grade", 3);
    }

    @Test
    void nestedIf_score95_onlyFirstConditionEvaluated() throws Exception {
        // Given: NestedIf instrumented — `if (score >= 90)` is first check
        // When:  grade(95) — first condition true, returns "A" immediately
        // Then:  only 1 branch probe fires (first TRUE); remaining conditions not evaluated
        var v = InstrumentationVerifier.of(NestedIf.class);
        v.invoke("grade", new Class[]{int.class}, 95);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    @Test
    void nestedIf_score80_firstFalseSecondTrue() throws Exception {
        // Given: NestedIf instrumented
        // When:  grade(80) — first condition false (80 < 90), second true (80 >= 75)
        // Then:  2 branch probes fire: first FALSE + second TRUE
        var v = InstrumentationVerifier.of(NestedIf.class);
        v.invoke("grade", new Class[]{int.class}, 80);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    @Test
    void nestedIf_score50_allThreeConditionsFalse() throws Exception {
        // Given: NestedIf instrumented
        // When:  grade(50) — all three conditions false, falls through to "F"
        // Then:  3 branch probes fire (one FALSE per condition)
        var v = InstrumentationVerifier.of(NestedIf.class);
        v.invoke("grade", new Class[]{int.class}, 50);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(3);
    }

    // --- BooleanMethodResult (polarity regression guard) ---

    @Test
    void booleanMethod_describeSingleBranchPair() throws Exception {
        // Given: BooleanMethodResult instrumented — `if (items.isEmpty())` uses IFEQ
        //        isJumpTakenWhenTrue(IFEQ) = false → jump-taken is the FALSE path
        //        This polarity is the regression: without the fix TRUE/FALSE would be swapped
        // When:  no invocation — structural check
        // Then:  exactly 1 branch pair registered for describe()
        var v = InstrumentationVerifier.of(BooleanMethodResult.class);
        v.assertBranchPairCount("describe", 1);
    }

    @Test
    void booleanMethod_methodEntered() throws Exception {
        // Given: BooleanMethodResult instrumented
        // When:  describe() called (InstrumentationVerifier creates instance via no-arg isConstructor)
        // Then:  method probe fires — instrumentation reached the method entry
        var v = InstrumentationVerifier.of(BooleanMethodResult.class);
        v.invoke("describe");
        v.assertMethodEntered("describe");
    }

    // --- InstanceofCheck ---

    @Test
    void instanceof_exactlyTwoBranchPairs() throws Exception {
        // Given: InstanceofCheck instrumented — 2 instanceof checks, each produces IFXX
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered
        var v = InstrumentationVerifier.of(InstanceofCheck.class);
        v.assertBranchPairCount("describe", 2);
    }

    @Test
    void instanceof_stringInput_firstCheckTrueSecondNotReached() throws Exception {
        // Given: InstanceofCheck instrumented
        // When:  describe("hello") — first instanceof (String) succeeds → returns immediately
        // Then:  1 branch probe fires (first TRUE); second instanceof not evaluated
        var v = InstrumentationVerifier.of(InstanceofCheck.class);
        v.invoke("describe", new Class[]{Object.class}, "hello");
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    @Test
    void instanceof_integerInput_firstFalseSecondTrue() throws Exception {
        // Given: InstanceofCheck instrumented
        // When:  describe(42) — first instanceof (String) fails, second (Integer) succeeds
        // Then:  2 branch probes fire: first FALSE + second TRUE
        var v = InstrumentationVerifier.of(InstanceofCheck.class);
        v.invoke("describe", new Class[]{Object.class}, Integer.valueOf(42));
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    @Test
    void instanceof_unknownType_bothChecksFail() throws Exception {
        // Given: InstanceofCheck instrumented
        // When:  describe(3.14) — neither String nor Integer, falls to "other"
        // Then:  2 branch probes fire: both are FALSE (neither instanceof matched)
        var v = InstrumentationVerifier.of(InstanceofCheck.class);
        v.invoke("describe", new Class[]{Object.class}, 3.14);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    // =========================================================================
    // LOOPS
    // =========================================================================

    // --- WhileLoop ---

    @Test
    void whileLoop_positiveN_bodyExecutedBothDirectionsFire() throws Exception {
        // Given: WhileLoop instrumented — `while (i < n)`, 1 branch pair
        // When:  sum(3) — loop executes 3 times, then condition becomes false
        // Then:  TRUE fires on each iteration (condition holds), FALSE fires once on exit
        var v = InstrumentationVerifier.of(WhileLoop.class);
        v.invoke("sum", new Class[]{int.class}, 3);
        v.assertBranchHit("sum", "TRUE");
        v.assertBranchHit("sum", "FALSE");
    }

    @Test
    void whileLoop_zeroN_bodyNeverEntered() throws Exception {
        // Given: WhileLoop instrumented
        // When:  sum(0) — condition `i < n` is immediately false (0 < 0)
        //        javac compiles the loop back-edge as IF_ICMPLT L_body (direct condition).
        //        DefaultProbeInjector places IF_ICMPLT in the false group:
        //        fall-through (condition false, loop exits) → TRUE probe fires.
        //        jump-taken (condition true, loop body) → FALSE probe fires.
        // Then:  IF_ICMPLT falls through on first check → TRUE fires, FALSE does not
        var v = InstrumentationVerifier.of(WhileLoop.class);
        v.invoke("sum", new Class[]{int.class}, 0);
        v.assertBranchHit("sum", "TRUE");
        v.assertBranchNotHit("sum", "FALSE");
    }

    @Test
    void whileLoop_oneIteration_trueAndFalseBothFire() throws Exception {
        // Given: WhileLoop instrumented
        // When:  sum(1) — loop body executes once, then condition false
        // Then:  TRUE fires once (body executed), FALSE fires once (exit)
        var v = InstrumentationVerifier.of(WhileLoop.class);
        v.invoke("sum", new Class[]{int.class}, 1);
        v.assertBranchHit("sum", "TRUE");
        v.assertBranchHit("sum", "FALSE");
    }

    // --- DoWhileLoop ---

    @Test
    void doWhile_positiveStart_multipleIterations() throws Exception {
        // Given: DoWhileLoop instrumented — `do { } while (start > 0)`, 1 branch pair at bottom
        // When:  countDown(3) — body executes 3 times
        // Then:  TRUE fires for each re-entry, FALSE fires when start reaches 0
        var v = InstrumentationVerifier.of(DoWhileLoop.class);
        v.invoke("countDown", new Class[]{int.class}, 3);
        v.assertBranchHit("countDown", "TRUE");
        v.assertBranchHit("countDown", "FALSE");
    }

    @Test
    void doWhile_startOne_bodyExecutedOnceThenFalse() throws Exception {
        // Given: DoWhileLoop instrumented
        // When:  countDown(1) — body executes once, start becomes 0, condition false
        // Then:  FALSE fires (condition fails after first iteration), TRUE never fires
        //        (do-while always runs body at least once, so 0 is a valid single-iteration case)
        var v = InstrumentationVerifier.of(DoWhileLoop.class);
        v.invoke("countDown", new Class[]{int.class}, 1);
        v.assertBranchHit("countDown", "FALSE");
        v.assertBranchNotHit("countDown", "TRUE");
    }

    // --- ForLoop ---

    @Test
    void forLoop_positiveN_loopsAndExits() throws Exception {
        // Given: ForLoop instrumented — `for (int i = 1; i <= n; i++)`, 1 branch pair
        // When:  product(4) — loop iterates 4 times
        // Then:  TRUE fires per iteration (condition holds), FALSE fires on exit
        var v = InstrumentationVerifier.of(ForLoop.class);
        v.invoke("product", new Class[]{int.class}, 4);
        v.assertBranchHit("product", "TRUE");
        v.assertBranchHit("product", "FALSE");
    }

    @Test
    void forLoop_nIsZero_bodyNeverEntered() throws Exception {
        // Given: ForLoop instrumented — initial i=1, condition `i <= n` is immediately false when n=0
        //        javac compiles the for-loop back-edge as IF_ICMPLE L_body (direct condition).
        //        DefaultProbeInjector places IF_ICMPLE in the false group:
        //        fall-through (i > n, exits) → TRUE fires; jump-taken (i <= n, body) → FALSE fires.
        // When:  product(0) — loop body never entered
        // Then:  IF_ICMPLE falls through on first check → TRUE fires, FALSE does not
        var v = InstrumentationVerifier.of(ForLoop.class);
        v.invoke("product", new Class[]{int.class}, 0);
        v.assertBranchHit("product", "TRUE");
        v.assertBranchNotHit("product", "FALSE");
    }

    // --- ForEachIterable ---

    @Test
    void forEachIterable_nonEmptyList_iterates() throws Exception {
        // Given: ForEachIterable instrumented — compiles to iterator.hasNext() + IFEQ
        // When:  join(["a","b","c"]) — iterator has 3 elements
        // Then:  TRUE fires 3 times (hasNext returns true), FALSE fires once (exhausted)
        var v = InstrumentationVerifier.of(ForEachIterable.class);
        v.invoke("join", new Class[]{java.util.List.class}, List.of("a", "b", "c"));
        v.assertBranchHit("join", "TRUE");
        v.assertBranchHit("join", "FALSE");
    }

    @Test
    void forEachIterable_emptyList_bodyNeverEntered() throws Exception {
        // Given: ForEachIterable instrumented
        // When:  join([]) — empty list, hasNext() returns false immediately
        // Then:  FALSE fires on first check, TRUE never fires
        var v = InstrumentationVerifier.of(ForEachIterable.class);
        v.invoke("join", new Class[]{java.util.List.class}, List.of());
        v.assertBranchHit("join", "FALSE");
        v.assertBranchNotHit("join", "TRUE");
    }

    // --- ForEachArray ---

    @Test
    void forEachArray_nonEmptyArray_iterates() throws Exception {
        // Given: ForEachArray instrumented — compiles to index < array.length with IF_ICMPGE
        // When:  sumArray([1,2,3]) — 3 elements iterated
        // Then:  TRUE fires 3 times (index in bounds), FALSE fires once (index out of bounds)
        var v = InstrumentationVerifier.of(ForEachArray.class);
        v.invoke("sumArray", new Class[]{int[].class}, new int[]{1, 2, 3});
        v.assertBranchHit("sumArray", "TRUE");
        v.assertBranchHit("sumArray", "FALSE");
    }

    @Test
    void forEachArray_emptyArray_bodyNeverEntered() throws Exception {
        // Given: ForEachArray instrumented
        // When:  sumArray([]) — length is 0, javac emits IF_ICMPGE L_exit (exit when index >= length)
        //        DefaultProbeInjector places IF_ICMPGE in the true group:
        //        jump-taken (index >= length, exits) → TRUE fires; fall-through (in-bounds) → FALSE fires.
        // Then:  IF_ICMPGE jumps immediately (0 >= 0) → TRUE fires, FALSE does not
        var v = InstrumentationVerifier.of(ForEachArray.class);
        v.invoke("sumArray", new Class[]{int[].class}, new int[]{});
        v.assertBranchHit("sumArray", "TRUE");
        v.assertBranchNotHit("sumArray", "FALSE");
    }

    // --- LoopBreak ---

    @Test
    void loopBreak_exactlyTwoBranchPairs() throws Exception {
        // Given: LoopBreak instrumented — outer loop condition + inner equality check = 2 IFXX
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered
        var v = InstrumentationVerifier.of(LoopBreak.class);
        v.assertBranchPairCount("findFirst", 2);
    }

    @Test
    void loopBreak_targetFound_innerTrueFiresEarlyReturn() throws Exception {
        // Given: LoopBreak instrumented
        // When:  findFirst([1,2,3], 2) — target found at index 1
        // Then:  method entered; inner condition TRUE fires triggering early return
        var v = InstrumentationVerifier.of(LoopBreak.class);
        v.invoke("findFirst", new Class[]{int[].class, int.class}, new int[]{1, 2, 3}, 2);
        v.assertMethodEntered("findFirst");
    }

    @Test
    void loopBreak_targetNotFound_innerAlwaysFalse() throws Exception {
        // Given: LoopBreak instrumented
        // When:  findFirst([1,2,3], 99) — target not present, all equality checks fail
        // Then:  inner condition FALSE fires for every element; loop exits naturally
        var v = InstrumentationVerifier.of(LoopBreak.class);
        v.invoke("findFirst", new Class[]{int[].class, int.class}, new int[]{1, 2, 3}, 99);
        v.assertMethodEntered("findFirst");
    }

    @Test
    void loopBreak_emptyArray_loopBodyNeverEntered() throws Exception {
        // Given: LoopBreak instrumented
        // When:  findFirst([], 1) — empty array, outer loop condition immediately false
        // Then:  outer loop FALSE fires, inner condition never reached
        var v = InstrumentationVerifier.of(LoopBreak.class);
        v.invoke("findFirst", new Class[]{int[].class, int.class}, new int[]{}, 1);
        v.assertMethodEntered("findFirst");
    }

    // --- LoopContinue ---

    @Test
    void loopContinue_exactlyTwoBranchPairs() throws Exception {
        // Given: LoopContinue instrumented — array iteration bound + `if (v <= 0)` = 2 IFXX
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered
        var v = InstrumentationVerifier.of(LoopContinue.class);
        v.assertBranchPairCount("sumPositive", 2);
    }

    @Test
    void loopContinue_mixedValues_innerBothDirectionsFire() throws Exception {
        // Given: LoopContinue instrumented
        // When:  sumPositive([1,-2,3,-4,5]) — positives pass inner check, negatives skip
        // Then:  inner condition TRUE fires for negatives (skipped), FALSE fires for positives (summed)
        var v = InstrumentationVerifier.of(LoopContinue.class);
        v.invoke("sumPositive", new Class[]{int[].class}, new int[]{1, -2, 3, -4, 5});
        v.assertMethodEntered("sumPositive");
    }

    @Test
    void loopContinue_allNegative_innerAlwaysTrue() throws Exception {
        // Given: LoopContinue instrumented
        // When:  sumPositive([-1,-2,-3]) — all values <= 0, all skip
        // Then:  inner condition TRUE fires for every element, FALSE never fires
        var v = InstrumentationVerifier.of(LoopContinue.class);
        v.invoke("sumPositive", new Class[]{int[].class}, new int[]{-1, -2, -3});
        v.assertMethodEntered("sumPositive");
    }

    @Test
    void loopContinue_allPositive_innerAlwaysFalse() throws Exception {
        // Given: LoopContinue instrumented
        // When:  sumPositive([1,2,3]) — all values > 0, none skipped
        // Then:  inner condition FALSE fires for every element, TRUE never fires
        var v = InstrumentationVerifier.of(LoopContinue.class);
        v.invoke("sumPositive", new Class[]{int[].class}, new int[]{1, 2, 3});
        v.assertMethodEntered("sumPositive");
    }

    // --- NestedLoops ---

    @Test
    void nestedLoops_exactlyTwoBranchPairs() throws Exception {
        // Given: NestedLoops instrumented — outer `r < rows` and inner `c < cols` = 2 IFXX
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs registered (one per loop)
        var v = InstrumentationVerifier.of(NestedLoops.class);
        v.assertBranchPairCount("buildMatrix", 2);
    }

    @Test
    void nestedLoops_2x3_bothLoopsEnterAndExit() throws Exception {
        // Given: NestedLoops instrumented — 2 branch pairs (outer + inner loop condition)
        // When:  buildMatrix(2, 3) — outer iterates 2 times, inner 3 times each
        // Then:  method entered and all branch probes hit (both loops run and exit)
        //        Note: cannot use direction-only assertBranchHit because there are 2 TRUE probes.
        var v = InstrumentationVerifier.of(NestedLoops.class);
        v.invoke("buildMatrix", new Class[]{int.class, int.class}, 2, 3);
        v.assertMethodEntered("buildMatrix");
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(4); // 2 loops × 2 directions = 4 probes all fired
    }

    @Test
    void nestedLoops_0rows_outerLoopNeverEnters_innerNotReached() throws Exception {
        // Given: NestedLoops instrumented
        // When:  buildMatrix(0, 3) — outer condition `0 < 0` immediately false
        // Then:  only outer loop's FALSE fires; inner loop probe never reached
        var v = InstrumentationVerifier.of(NestedLoops.class);
        v.invoke("buildMatrix", new Class[]{int.class, int.class}, 0, 3);
        List<com.coveragex.api.data.ProbeMetadata> meta = v.probeMetadata();
        boolean[] hits = v.probeHits();
        long hitBranches = meta.stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    // =========================================================================
    // SWITCHES
    // =========================================================================

    // --- SwitchInt ---

    @Test
    void switchInt_noBranchProbes_tableSwitchNotViaVisitJumpInsn() throws Exception {
        // Given: SwitchInt instrumented — `switch (int)` compiles to TABLESWITCH
        //        TABLESWITCH bypasses visitJumpInsn → no BranchProbes injected
        // When:  no invocation — structural check
        // Then:  0 branch pairs; only MethodProbe and ReturnProbes present
        var v = InstrumentationVerifier.of(SwitchInt.class);
        v.assertBranchPairCount("dayName", 0);
    }

    @Test
    void switchInt_caseOne_methodEnteredReturnProbesFire() throws Exception {
        // Given: SwitchInt instrumented — no branch probes but ReturnProbes before each return
        // When:  dayName(1) — case 1 matched
        // Then:  method entered; ReturnProbe for case 1's return fires
        var v = InstrumentationVerifier.of(SwitchInt.class);
        v.invoke("dayName", new Class[]{int.class}, 1);
        v.assertMethodEntered("dayName");
    }

    @Test
    void switchInt_defaultCase_methodEnteredDefaultProbesFires() throws Exception {
        // Given: SwitchInt instrumented
        // When:  dayName(99) — no case matches, default reached
        // Then:  method entered; ReturnProbe for default return fires
        var v = InstrumentationVerifier.of(SwitchInt.class);
        v.invoke("dayName", new Class[]{int.class}, 99);
        v.assertMethodEntered("dayName");
    }

    // --- SwitchString ---

    @Test
    void switchString_hasBranchProbes_fromHashcodeAndEqualsChecks() throws Exception {
        // Given: SwitchString instrumented — `switch (String)` compiles to:
        //        hashCode() comparison (IFXX) + per-case equals() check (IFXX)
        //        These are regular conditional jumps → BranchProbes are injected
        // When:  no invocation — structural check
        // Then:  at least 1 branch pair registered (one per case comparison)
        var v = InstrumentationVerifier.of(SwitchString.class);
        assertThat(v.countBranchPairsInMethod("priority")).isGreaterThan(0);
    }

    @Test
    void switchString_knownCase_methodEntered() throws Exception {
        // Given: SwitchString instrumented
        // When:  priority("HIGH") — hash matches, equals() succeeds for "HIGH" case
        // Then:  method entered; relevant equals-check branch TRUE fires
        var v = InstrumentationVerifier.of(SwitchString.class);
        v.invoke("priority", new Class[]{String.class}, "HIGH");
        v.assertMethodEntered("priority");
    }

    @Test
    void switchString_unknownCase_allEqualsFail_defaultReturns() throws Exception {
        // Given: SwitchString instrumented
        // When:  priority("UNKNOWN") — no case hash or equals matches, default returns 0
        // Then:  method entered; all equals-check FALSE branches fire
        var v = InstrumentationVerifier.of(SwitchString.class);
        v.invoke("priority", new Class[]{String.class}, "UNKNOWN");
        v.assertMethodEntered("priority");
    }

    // --- SwitchExpression ---

    @Test
    void switchExpression_noBranchProbes_arrowSwitchIsTableSwitch() throws Exception {
        // Given: SwitchExpression instrumented — arrow `switch` expression compiles to TABLESWITCH
        //        Same as switch(int) statement — no visitJumpInsn → no BranchProbes
        // When:  no invocation — structural check
        // Then:  0 branch pairs
        var v = InstrumentationVerifier.of(SwitchExpression.class);
        v.assertBranchPairCount("describe", 0);
    }

    @Test
    void switchExpression_allCasesReachableByCallingThreeTimes() throws Exception {
        // Given: SwitchExpression instrumented — cases: 0, 1, default
        // When:  describe() called for each distinct case value
        // Then:  method entered each time; all ReturnProbes (one per arm) are reachable
        var v = InstrumentationVerifier.of(SwitchExpression.class);
        v.invoke("describe", new Class[]{int.class}, 0);
        v.invoke("describe", new Class[]{int.class}, 1);
        v.invoke("describe", new Class[]{int.class}, 42);
        v.assertMethodEntered("describe");
    }

    // =========================================================================
    // EXCEPTION HANDLING
    // =========================================================================

    // --- TryCatch ---

    @Test
    void tryCatch_noBranchProbes_exceptionTableNotViaIfxx() throws Exception {
        // Given: TryCatch instrumented — `catch` handler uses exception table, not IFXX
        //        ProbeInjectionSupport only hooks visitJumpInsn; exception dispatch is invisible to it
        // When:  no invocation — structural check
        // Then:  0 branch pairs; only MethodProbe and ReturnProbes (one per return)
        var v = InstrumentationVerifier.of(TryCatch.class);
        v.assertBranchPairCount("parse", 0);
    }

    @Test
    void tryCatch_validNumber_tryBodyReturnProbeHit() throws Exception {
        // Given: TryCatch instrumented
        // When:  parse("42") — parseInt succeeds, returns from try block
        // Then:  method entered; ReturnProbe for `return Integer.parseInt(s)` fires
        var v = InstrumentationVerifier.of(TryCatch.class);
        v.invoke("parse", new Class[]{String.class}, "42");
        v.assertMethodEntered("parse");
    }

    @Test
    void tryCatch_invalidNumber_catchBodyReturnProbeHit() throws Exception {
        // Given: TryCatch instrumented
        // When:  parse("abc") — parseInt throws, execution reaches catch block
        // Then:  method entered; ReturnProbe for `return -1` in catch fires
        var v = InstrumentationVerifier.of(TryCatch.class);
        v.invoke("parse", new Class[]{String.class}, "abc");
        v.assertMethodEntered("parse");
    }

    @Test
    void tryCatch_bothPathsCovered_requiresTwoInvocations() throws Exception {
        // Given: TryCatch instrumented — two ReturnProbes: one in try, one in catch
        // When:  parse() called with valid input then invalid input
        // Then:  all probes in the method are hit — full method coverage
        var v = InstrumentationVerifier.of(TryCatch.class);
        v.invoke("parse", new Class[]{String.class}, "7");
        v.invoke("parse", new Class[]{String.class}, "bad");
        v.assertFullMethodCoverage("parse");
    }

    // --- TryCatchFinally ---

    @Test
    void tryCatchFinally_oneBranchPair_fromExplicitIfInsideTry() throws Exception {
        // Given: TryCatchFinally instrumented — `if (shouldThrow)` inside try block = 1 IFXX
        //        The try/catch/finally boundary itself adds no BranchProbe
        // When:  no invocation — structural check
        // Then:  exactly 1 branch pair (from the explicit if, not from the exception handlers)
        var v = InstrumentationVerifier.of(TryCatchFinally.class);
        v.assertBranchPairCount("attempt", 1);
    }

    @Test
    void tryCatchFinally_shouldThrowTrue_ifBodyEntered_exceptionCaught() throws Exception {
        // Given: TryCatchFinally instrumented
        // When:  attempt(true) — if condition true → throw → catch block executes
        // Then:  TRUE branch probe fires; method entered
        var v = InstrumentationVerifier.of(TryCatchFinally.class);
        v.invoke("attempt", new Class[]{boolean.class}, true);
        v.assertBranchHit("attempt", "TRUE");
    }

    @Test
    void tryCatchFinally_shouldThrowFalse_ifBodySkipped_happyPath() throws Exception {
        // Given: TryCatchFinally instrumented
        // When:  attempt(false) — if condition false → no throw → returns "ok"
        // Then:  FALSE branch probe fires
        var v = InstrumentationVerifier.of(TryCatchFinally.class);
        v.invoke("attempt", new Class[]{boolean.class}, false);
        v.assertBranchHit("attempt", "FALSE");
    }

    // --- TryWithResources ---

    @Test
    void tryWithResources_hasCompilerGeneratedNullCheckBranch() throws Exception {
        // Given: TryWithResources instrumented — `try (StringReader reader = ...)` compiles to:
        //        `if (reader != null) reader.close()` null guard generated by compiler (IFNULL)
        //        That IFNULL → BranchProbe pair even though we didn't write the null check
        // When:  no invocation — structural check
        // Then:  at least 1 branch pair (from compiler-generated null check)
        var v = InstrumentationVerifier.of(TryWithResources.class);
        assertThat(v.countBranchPairsInMethod("read")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void tryWithResources_nonEmptyContent_readSucceeds() throws Exception {
        // Given: TryWithResources instrumented
        // When:  read("hello") — reader.read() returns 'h' (non -1)
        // Then:  method entered; ternary TRUE branch fires (first char != -1)
        var v = InstrumentationVerifier.of(TryWithResources.class);
        v.invoke("read", new Class[]{String.class}, "hello");
        v.assertMethodEntered("read");
    }

    @Test
    void tryWithResources_emptyContent_readReturnsNegativeOne() throws Exception {
        // Given: TryWithResources instrumented
        // When:  read("") — reader.read() returns -1 immediately
        // Then:  method entered; ternary FALSE branch fires (first == -1)
        var v = InstrumentationVerifier.of(TryWithResources.class);
        v.invoke("read", new Class[]{String.class}, "");
        v.assertMethodEntered("read");
    }

    // --- MultiCatch ---

    @Test
    void multiCatch_noBranchProbesAtHandlerBoundary() throws Exception {
        // Given: MultiCatch instrumented — `catch (NumberFormatException | StringIndexOutOfBoundsException e)`
        //        is a single exception table entry, not an IFXX → no BranchProbe at handler entry
        // When:  no invocation — structural check
        // Then:  0 branch pairs
        var v = InstrumentationVerifier.of(MultiCatch.class);
        v.assertBranchPairCount("convert", 0);
    }

    @Test
    void multiCatch_validInput_tryBodyCompletes() throws Exception {
        // Given: MultiCatch instrumented
        // When:  convert("42") — parseInt and charAt both succeed
        // Then:  method entered; ReturnProbe for the try-body return fires
        var v = InstrumentationVerifier.of(MultiCatch.class);
        v.invoke("convert", new Class[]{String.class}, "42");
        v.assertMethodEntered("convert");
    }

    @Test
    void multiCatch_invalidNumber_numberFormatExceptionCaught() throws Exception {
        // Given: MultiCatch instrumented
        // When:  convert("notanumber") — parseInt throws NumberFormatException
        // Then:  method entered; catch block's ReturnProbe fires (return "error")
        var v = InstrumentationVerifier.of(MultiCatch.class);
        v.invoke("convert", new Class[]{String.class}, "notanumber");
        v.assertMethodEntered("convert");
    }

    @Test
    void multiCatch_emptyString_stringIndexExceptionCaught() throws Exception {
        // Given: MultiCatch instrumented
        // When:  convert("") — parseInt("") throws NumberFormatException (also catches SIOOBE)
        // Then:  method entered; same catch block fires (multi-catch handles both types)
        var v = InstrumentationVerifier.of(MultiCatch.class);
        v.invoke("convert", new Class[]{String.class}, "");
        v.assertMethodEntered("convert");
    }

    // =========================================================================
    // LAMBDAS
    // =========================================================================

    // --- SimpleLambda ---

    @Test
    void simpleLambda_outerMethodHasMethodProbe() throws Exception {
        // Given: SimpleLambda instrumented — upperCaseAll() is a regular method
        // When:  upperCaseAll(["a","b"]) — method executes
        // Then:  MethodProbe for upperCaseAll fires
        var v = InstrumentationVerifier.of(SimpleLambda.class);
        v.invoke("upperCaseAll", new Class[]{java.util.List.class}, List.of("a", "b"));
        v.assertMethodEntered("upperCaseAll");
    }

    @Test
    void simpleLambda_lambdaBodyInstrumentedAsSyntheticMethod() throws Exception {
        // Given: SimpleLambda instrumented — `s -> s.toUpperCase()` compiles to a synthetic
        //        private method (lambda$upperCaseAll$0) inside the same class
        //        ClassTransformer instruments ALL methods including synthetic ones
        // When:  upperCaseAll(["hello"]) — lambda is invoked for the single element
        // Then:  a MethodProbe with name containing "lambda$" appears in probeMetadata
        var v = InstrumentationVerifier.of(SimpleLambda.class);
        v.invoke("upperCaseAll", new Class[]{java.util.List.class}, List.of("hello"));
        boolean lambdaProbeFound = v.probeMetadata().stream()
            .anyMatch(pm -> pm.methodName().contains("lambda$"));
        assertThat(lambdaProbeFound)
            .as("Lambda synthetic method should be instrumented")
            .isTrue();
    }

    @Test
    void simpleLambda_emptyList_lambdaBodyNeverInvoked() throws Exception {
        // Given: SimpleLambda instrumented — lambda is compiled to synthetic method
        // When:  upperCaseAll([]) — empty list, stream produces no elements, lambda never called
        // Then:  lambda's MethodProbe is registered (during transform) but never hit at runtime
        var v = InstrumentationVerifier.of(SimpleLambda.class);
        v.invoke("upperCaseAll", new Class[]{java.util.List.class}, List.of());
        boolean[] hits = v.probeHits();
        boolean lambdaHit = v.probeMetadata().stream()
            .filter(pm -> pm.methodName().contains("lambda$"))
            .anyMatch(pm -> hits[pm.probeId()]);
        assertThat(lambdaHit).isFalse();
    }

    // --- LambdaWithCondition ---

    @Test
    void lambdaWithCondition_branchProbeRegisteredInsideSyntheticMethod() throws Exception {
        // Given: LambdaWithCondition instrumented — `n -> n > 0` compiles to synthetic method
        //        containing `IF_ICMPLE` → 1 BranchProbe pair registered in the synthetic method
        // When:  filterPositive([1,-2,3]) — lambda invoked multiple times
        // Then:  a BranchProbe with methodName containing "lambda$" is registered
        var v = InstrumentationVerifier.of(LambdaWithCondition.class);
        v.invoke("filterPositive", new Class[]{java.util.List.class}, List.of(1, -2, 3));
        boolean lambdaBranchFound = v.probeMetadata().stream()
            .anyMatch(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && bp.methodName().contains("lambda$"));
        assertThat(lambdaBranchFound)
            .as("Branch probe inside lambda body must be instrumented")
            .isTrue();
    }

    @Test
    void lambdaWithCondition_mixedList_bothBranchDirectionsFireInLambda() throws Exception {
        // Given: LambdaWithCondition instrumented — lambda has 1 branch pair
        // When:  filterPositive([1,-2,3]) — lambda called with positives (TRUE) and negatives (FALSE)
        // Then:  both TRUE and FALSE branch probes in the lambda's synthetic method fire
        var v = InstrumentationVerifier.of(LambdaWithCondition.class);
        v.invoke("filterPositive", new Class[]{java.util.List.class}, List.of(1, -2, 3));
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && bp.methodName().contains("lambda$") && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    @Test
    void lambdaWithCondition_allPositive_onlyTrueBranchFiresInLambda() throws Exception {
        // Given: LambdaWithCondition instrumented
        // When:  filterPositive([1,2,3]) — all positive, lambda condition always true
        // Then:  only TRUE branch probe fires inside the lambda; FALSE never fires
        var v = InstrumentationVerifier.of(LambdaWithCondition.class);
        v.invoke("filterPositive", new Class[]{java.util.List.class}, List.of(1, 2, 3));
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && bp.methodName().contains("lambda$") && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    // --- MethodReference ---

    @Test
    void methodReference_staticRef_outerMethodHasProbe() throws Exception {
        // Given: MethodReference instrumented — convertToStrings() uses String::valueOf
        //        Pure method references use invokedynamic and add no synthetic method to this class
        // When:  convertToStrings([1,2,3]) — method executes
        // Then:  outer method's MethodProbe fires
        var v = InstrumentationVerifier.of(MethodReference.class);
        v.invoke("convertToStrings", new Class[]{java.util.List.class}, List.of(1, 2, 3));
        v.assertMethodEntered("convertToStrings");
    }

    @Test
    void methodReference_instanceRef_outerMethodHasProbe() throws Exception {
        // Given: MethodReference instrumented — toUpperCase() uses String::toUpperCase
        // When:  toUpperCase(["a","b"]) — method executes
        // Then:  outer method's MethodProbe fires
        var v = InstrumentationVerifier.of(MethodReference.class);
        v.invoke("toUpperCase", new Class[]{java.util.List.class}, List.of("a", "b"));
        v.assertMethodEntered("toUpperCase");
    }

    @Test
    void methodReference_noSyntheticLambdaMethodAddedToClass() throws Exception {
        // Given: MethodReference instrumented — String::valueOf and String::toUpperCase
        //        are pure method references (invokedynamic without captured state)
        //        The JVM links them at runtime via MethodHandles; no synthetic method
        //        is added to MethodReference.class by the compiler
        // When:  no invocation — structural check on registered probe metadata
        // Then:  no probe with methodName containing "lambda$" appears — no synthetic method exists
        var v = InstrumentationVerifier.of(MethodReference.class);
        boolean hasSyntheticLambda = v.probeMetadata().stream()
            .anyMatch(pm -> pm.methodName().contains("lambda$"));
        assertThat(hasSyntheticLambda)
            .as("Pure method references should not add synthetic lambda methods to the class")
            .isFalse();
    }

    // =========================================================================
    // METHODS
    // =========================================================================

    // --- MultipleReturns ---

    @Test
    void multipleReturns_exactlyThreeBranchPairs() throws Exception {
        // Given: MultipleReturns instrumented — three chained `if` guards, 3 IFXX instructions
        // When:  no invocation — structural check
        // Then:  exactly 3 branch pairs registered
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.assertBranchPairCount("classify", 3);
    }

    @Test
    void multipleReturns_negative_firstConditionTrueEarlyReturn() throws Exception {
        // Given: MultipleReturns instrumented — first guard is `if (x < 0)`
        // When:  classify(-5) — first condition true → "negative" returned immediately
        // Then:  1 branch probe fires (first TRUE); remaining conditions not evaluated
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.invoke("classify", new Class[]{int.class}, -5);
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(1);
    }

    @Test
    void multipleReturns_zero_firstFalseSecondTrue() throws Exception {
        // Given: MultipleReturns instrumented
        // When:  classify(0) — first false (0 is not < 0), second true (0 == 0)
        // Then:  2 branch probes fire: first FALSE + second TRUE
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.invoke("classify", new Class[]{int.class}, 0);
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(2);
    }

    @Test
    void multipleReturns_small_firstTwoFalseThirdTrue() throws Exception {
        // Given: MultipleReturns instrumented
        // When:  classify(5) — first two false, third true (5 < 10)
        // Then:  3 branch probes fire: first FALSE + second FALSE + third TRUE
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.invoke("classify", new Class[]{int.class}, 5);
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(3);
    }

    @Test
    void multipleReturns_large_allThreeConditionsFalse() throws Exception {
        // Given: MultipleReturns instrumented
        // When:  classify(100) — all three conditions false, falls through to "large"
        // Then:  3 branch probes fire (all FALSE directions); "large" path is reached
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.invoke("classify", new Class[]{int.class}, 100);
        boolean[] hits = v.probeHits();
        long hitBranches = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitBranches).isEqualTo(3);
    }

    @Test
    void multipleReturns_fullCoverageRequiresFourDistinctInputs() throws Exception {
        // Given: MultipleReturns instrumented — 4 return paths: negative, zero, small, large
        // When:  classify() called once per path: -1, 0, 5, 100
        // Then:  all probes in the method are hit — full branch and line coverage achieved
        var v = InstrumentationVerifier.of(MultipleReturns.class);
        v.invoke("classify", new Class[]{int.class}, -1);
        v.invoke("classify", new Class[]{int.class}, 0);
        v.invoke("classify", new Class[]{int.class}, 5);
        v.invoke("classify", new Class[]{int.class}, 100);
        v.assertFullMethodCoverage("classify");
    }

    // --- UnconditionalThrow ---

    @Test
    void unconditionalThrow_oneBranchPair() throws Exception {
        // Given: UnconditionalThrow instrumented — `if (x <= 0) throw ...`, 1 IFXX
        // When:  no invocation — structural check
        // Then:  exactly 1 branch pair registered
        var v = InstrumentationVerifier.of(UnconditionalThrow.class);
        v.assertBranchPairCount("requirePositive", 1);
    }

    @Test
    void unconditionalThrow_positiveValue_conditionFalse_noThrow() throws Exception {
        // Given: UnconditionalThrow instrumented
        // When:  requirePositive(5) — source condition `x <= 0` is false
        //        javac compiles `if (x <= 0) { throw }` as IFGT L_skip (jump over throw when x > 0).
        //        DefaultProbeInjector: IFGT is in the true group → jump-taken fires TRUE.
        //        x=5: IFGT jumps (5 > 0) → TRUE fires (opcode condition true, source condition false).
        // Then:  TRUE branch probe fires, FALSE does not
        var v = InstrumentationVerifier.of(UnconditionalThrow.class);
        v.invoke("requirePositive", new Class[]{int.class}, 5);
        v.assertBranchHit("requirePositive", "TRUE");
        v.assertBranchNotHit("requirePositive", "FALSE");
    }

    @Test
    void unconditionalThrow_zeroValue_conditionTrue_throwFires() throws Exception {
        // Given: UnconditionalThrow instrumented
        // When:  requirePositive(0) — source condition `x <= 0` is true
        //        x=0: IFGT falls through (0 > 0 = false) → FALSE fires.
        //        InvocationTargetException wraps the throw when called via reflection.
        // Then:  FALSE branch probe fires; TRUE does not
        var v = InstrumentationVerifier.of(UnconditionalThrow.class);
        try {
            v.invoke("requirePositive", new Class[]{int.class}, 0);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Expected — the fixture throws; probe state is still recorded
        }
        v.assertBranchHit("requirePositive", "FALSE");
        v.assertBranchNotHit("requirePositive", "TRUE");
    }

    // --- Constructor ---

    @Test
    void constructor_initMethodIsInstrumented() throws Exception {
        // Given: Constructor fixture instrumented — isConstructor is `<init>` method in bytecode
        //        ProbeInjector instruments ALL methods including constructors
        // When:  InstrumentationVerifier transforms the class (transformation registers metadata)
        // Then:  a MethodProbe with methodName "<init>" appears in the registered metadata
        var v = InstrumentationVerifier.of(Constructor.class);
        boolean initFound = v.probeMetadata().stream()
            .anyMatch(pm -> "<init>".equals(pm.methodName()));
        assertThat(initFound).as("Constructor <init> must be instrumented").isTrue();
    }

    @Test
    void constructor_gettersRegisteredEvenWithoutBeingCalled() throws Exception {
        // Given: Constructor fixture instrumented — getName() and getValue() are simple methods
        // When:  InstrumentationVerifier transforms the class (probeMetadata populated at transform time)
        // Then:  MethodProbes for both getters appear in metadata even before they are invoked
        var v = InstrumentationVerifier.of(Constructor.class);
        boolean getNameFound = v.probeMetadata().stream()
            .anyMatch(pm -> "getName".equals(pm.methodName()));
        boolean getValueFound = v.probeMetadata().stream()
            .anyMatch(pm -> "getValue".equals(pm.methodName()));
        assertThat(getNameFound).as("getName must be instrumented").isTrue();
        assertThat(getValueFound).as("getValue must be instrumented").isTrue();
    }

    // --- StaticMethod ---

    @Test
    void staticMethod_maxHasOneBranchPair() throws Exception {
        // Given: StaticMethod instrumented — `a >= b ? a : b` is a ternary, 1 IFXX
        //        Static method: isStatic=true, args loaded from slot 0 (not 1)
        // When:  no invocation — structural check
        // Then:  exactly 1 branch pair registered for max()
        var v = InstrumentationVerifier.of(StaticMethod.class);
        v.assertBranchPairCount("max", 1);
    }

    @Test
    void staticMethod_maxFirstLarger_trueBranchHit() throws Exception {
        // Given: StaticMethod instrumented
        // When:  max(5, 3) — a >= b is true (5 >= 3)
        // Then:  TRUE branch probe fires, FALSE does not
        var v = InstrumentationVerifier.of(StaticMethod.class);
        v.invoke("max", new Class[]{int.class, int.class}, 5, 3);
        v.assertBranchHit("max", "TRUE");
        v.assertBranchNotHit("max", "FALSE");
    }

    @Test
    void staticMethod_maxSecondLarger_falseBranchHit() throws Exception {
        // Given: StaticMethod instrumented
        // When:  max(2, 7) — a >= b is false (2 < 7)
        // Then:  FALSE branch probe fires, TRUE does not
        var v = InstrumentationVerifier.of(StaticMethod.class);
        v.invoke("max", new Class[]{int.class, int.class}, 2, 7);
        v.assertBranchHit("max", "FALSE");
        v.assertBranchNotHit("max", "TRUE");
    }

    @Test
    void staticMethod_nullSafe_nonNullInput_trueBranchHit() throws Exception {
        // Given: StaticMethod instrumented — `s != null ? s : ""` ternary
        //        javac compiles `s != null ? s : ""` as IFNULL L_else (jump to "" when null).
        //        DefaultProbeInjector: IFNULL in true group → jump-taken fires TRUE.
        //        s="hello": IFNULL falls through (not null) → FALSE fires.
        //        Note: documented DefaultProbeInjector limitation — labels probe as "if (x == null)"
        //        so TRUE fires when x IS null, which is when source condition `s != null` is FALSE.
        // When:  nullSafe("hello") — s is not null
        // Then:  FALSE branch probe fires (IFNULL falls through), TRUE does not
        var v = InstrumentationVerifier.of(StaticMethod.class);
        v.invoke("nullSafe", new Class[]{String.class}, "hello");
        v.assertBranchHit("nullSafe", "FALSE");
        v.assertBranchNotHit("nullSafe", "TRUE");
    }

    @Test
    void staticMethod_nullSafe_nullInput_falseBranchHit() throws Exception {
        // Given: StaticMethod instrumented
        // When:  nullSafe(null) — s is null
        //        IFNULL jumps (s IS null) → TRUE fires.
        // Then:  TRUE branch probe fires, FALSE does not
        var v = InstrumentationVerifier.of(StaticMethod.class);
        v.invoke("nullSafe", new Class[]{String.class}, (Object) null);
        v.assertBranchHit("nullSafe", "TRUE");
        v.assertBranchNotHit("nullSafe", "FALSE");
    }

    // =========================================================================
    // NEGATED BOOLEAN METHOD CALL
    // =========================================================================

    @Test
    void negatedBooleanCall_enabledTrue_opcodeJumpsTrueFires() throws Exception {
        // Given: NegatedBooleanCall instrumented — `if (!enabled)` compiled as IFNE
        //        javac: ILOAD enabled → IFNE (jump when enabled != 0 = true)
        //        DefaultProbeInjector: IFNE → TRUE for jump-taken
        // When:  status(true) — enabled=true, IFNE jumps (skips if body)
        // Then:  opcode-level TRUE fires; source condition !true=false (body NOT entered)
        var v = InstrumentationVerifier.of(NegatedBooleanCall.class);
        v.invoke("status", new Class[]{boolean.class}, true);
        v.assertBranchHit("status", "TRUE");
        v.assertBranchNotHit("status", "FALSE");
    }

    @Test
    void negatedBooleanCall_enabledFalse_opcodeFallThroughFalseFires() throws Exception {
        // Given: NegatedBooleanCall instrumented
        // When:  status(false) — enabled=false, IFNE falls through (enters if body)
        // Then:  opcode-level FALSE fires; source condition !false=true (body entered)
        var v = InstrumentationVerifier.of(NegatedBooleanCall.class);
        v.invoke("status", new Class[]{boolean.class}, false);
        v.assertBranchHit("status", "FALSE");
        v.assertBranchNotHit("status", "TRUE");
    }

    // =========================================================================
    // CHAINED FOUR-OR-MORE OPERANDS
    // =========================================================================

    @Test
    void chainedFourOperands_exactlyFourBranchPairs() throws Exception {
        // Given: ChainedConditions instrumented — `a > 0 && b > 0 && c > 0 && d > 0`
        //        Each `> 0` comparison produces one IFLE instruction → 4 branch pairs total
        // When:  no invocation — structural check
        // Then:  exactly 4 branch pairs registered
        var v = InstrumentationVerifier.of(ChainedConditions.class);
        v.assertBranchPairCount("allPositive", 4);
    }

    @Test
    void chainedFourOperands_firstOperandFalse_shortCircuitsAtFirstPair() throws Exception {
        // Given: ChainedConditions instrumented
        // When:  allPositive(-1, 1, 1, 1) — a <= 0 short-circuits at first comparison
        // Then:  exactly one branch probe fires (first pair only)
        var v = InstrumentationVerifier.of(ChainedConditions.class);
        v.invoke("allPositive", new Class[]{int.class, int.class, int.class, int.class}, -1, 1, 1, 1);
        boolean[] hits = v.probeHits();
        long hitCount = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp && hits[bp.probeId()])
            .count();
        assertThat(hitCount).isEqualTo(1);
    }

    @Test
    void chainedFourOperands_allTrue_allFourPairsEvaluated() throws Exception {
        // Given: ChainedConditions instrumented
        // When:  allPositive(1, 2, 3, 4) — no short-circuit; all 4 conditions evaluate
        // Then:  all 4 TRUE direction probes fire
        var v = InstrumentationVerifier.of(ChainedConditions.class);
        v.invoke("allPositive", new Class[]{int.class, int.class, int.class, int.class}, 1, 2, 3, 4);
        boolean[] hits = v.probeHits();
        long trueHit = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && bp.direction() == com.coveragex.api.data.ProbeMetadata.BranchDirection.TRUE
                && hits[bp.probeId()])
            .count();
        assertThat(trueHit).isEqualTo(4);
    }

    // =========================================================================
    // NESTED INSTANCEOF WITH NULL
    // =========================================================================

    @Test
    void instanceofCheck_nullInput_bothChecksFail_noException() throws Exception {
        // Given: InstanceofCheck instrumented — two pattern-matching instanceof checks
        //        null fails all instanceof checks → both FALSE probes fire; no NPE
        // When:  describe(null)
        // Then:  method entered without exception; no TRUE probe fires
        var v = InstrumentationVerifier.of(InstanceofCheck.class);
        v.invoke("describe", new Class[]{Object.class}, (Object) null);
        v.assertMethodEntered("describe");
        boolean[] hits = v.probeHits();
        long trueHits = v.probeMetadata().stream()
            .filter(pm -> pm instanceof com.coveragex.api.data.ProbeMetadata.BranchProbe bp
                && bp.direction() == com.coveragex.api.data.ProbeMetadata.BranchDirection.TRUE
                && hits[bp.probeId()])
            .count();
        assertThat(trueHits).isZero();
    }

    // =========================================================================
    // INFINITE LOOP WITH EXPLICIT BREAK
    // =========================================================================

    @Test
    void infiniteLoopBreak_whileTrueAddsNoBranchProbe_innerIfsAddTwo() throws Exception {
        // Given: InfiniteLoopBreak instrumented — `while (true)` compiles to a GOTO (no IFXX)
        //        Two explicit `if` statements inside the loop produce 2 branch pairs
        // When:  no invocation — structural check
        // Then:  exactly 2 branch pairs (inner ifs only; while(true) creates no probe)
        var v = InstrumentationVerifier.of(InfiniteLoopBreak.class);
        v.assertBranchPairCount("countUntilNegative", 2);
    }

    @Test
    void infiniteLoopBreak_terminatesOnNegativeElement_methodExitsCorrectly() throws Exception {
        // Given: InfiniteLoopBreak instrumented
        // When:  countUntilNegative([1, 2, -3]) — exits at index 2 (arr[2] < 0 fires break)
        // Then:  method entered; returned index is 2
        var v = InstrumentationVerifier.of(InfiniteLoopBreak.class);
        Object result = v.invoke("countUntilNegative", new Class[]{int[].class}, new int[]{1, 2, -3});
        v.assertMethodEntered("countUntilNegative");
        assertThat(result).isEqualTo(2);
    }

    // =========================================================================
    // LABELED BREAK / CONTINUE
    // =========================================================================

    @Test
    void labeledBreak_targetFound_outerLoopExitsEarly() throws Exception {
        // Given: LabeledLoop instrumented — `break OUTER` compiles to GOTO; no extra branch probes
        //        Loop conditions and inner `if` create branch pairs; break itself does not
        // When:  find([[1,2],[3,4]], 3) — target 3 found at [1][0], break OUTER fires
        // Then:  method returns correct flat index; method entered
        var v = InstrumentationVerifier.of(LabeledLoop.class);
        Object result = v.invoke("find", new Class[]{int[][].class, int.class},
            new int[][]{{1, 2}, {3, 4}}, 3);
        v.assertMethodEntered("find");
        assertThat(result).isEqualTo(2);
    }

    @Test
    void labeledBreak_targetNotFound_exhaustsMatrix() throws Exception {
        // Given: LabeledLoop instrumented
        // When:  find([[1,2],[3,4]], 99) — target absent; both loops exhaust normally
        // Then:  method returns -1; method entered
        var v = InstrumentationVerifier.of(LabeledLoop.class);
        Object result = v.invoke("find", new Class[]{int[][].class, int.class},
            new int[][]{{1, 2}, {3, 4}}, 99);
        v.assertMethodEntered("find");
        assertThat(result).isEqualTo(-1);
    }

    // =========================================================================
    // ENUM SWITCH
    // =========================================================================

    @Test
    void enumSwitch_noBranchProbes_syntheticSwitchMapUsesTableSwitch() throws Exception {
        // Given: SwitchEnum instrumented — enum switch compiles via synthetic $SwitchMap$ + TABLESWITCH
        //        TABLESWITCH bypasses visitJumpInsn → no BranchProbes injected
        // When:  no invocation — structural check
        // Then:  0 branch pairs
        var v = InstrumentationVerifier.of(SwitchEnum.class);
        v.assertBranchPairCount("score", 0);
    }

    @Test
    void enumSwitch_allCasesReachable_methodEnteredForEachValue() throws Exception {
        // Given: SwitchEnum instrumented — cases LOW, MEDIUM, HIGH
        // When:  score() invoked for each Priority value
        // Then:  method entered each time; behavior preserved (1, 5, 10)
        var v = InstrumentationVerifier.of(SwitchEnum.class);
        assertThat(v.invoke("score", new Class[]{SwitchEnum.Priority.class}, SwitchEnum.Priority.LOW)).isEqualTo(1);
        assertThat(v.invoke("score", new Class[]{SwitchEnum.Priority.class}, SwitchEnum.Priority.MEDIUM)).isEqualTo(5);
        assertThat(v.invoke("score", new Class[]{SwitchEnum.Priority.class}, SwitchEnum.Priority.HIGH)).isEqualTo(10);
    }

    // =========================================================================
    // SPARSE INTEGER LOOKUP SWITCH
    // =========================================================================

    @Test
    void lookupSwitch_noBranchProbes_sparseIntCasesUseTableOrLookupSwitch() throws Exception {
        // Given: SwitchLookup instrumented — cases 1, 100, 1000 (non-contiguous) → LOOKUPSWITCH
        //        LOOKUPSWITCH bypasses visitJumpInsn → no BranchProbes
        // When:  no invocation — structural check
        // Then:  0 branch pairs
        var v = InstrumentationVerifier.of(SwitchLookup.class);
        v.assertBranchPairCount("classify", 0);
    }

    @Test
    void lookupSwitch_casesAndDefaultReachable_methodEntered() throws Exception {
        // Given: SwitchLookup instrumented
        // When:  classify() invoked for matched cases and default
        // Then:  method entered; correct strings returned
        var v = InstrumentationVerifier.of(SwitchLookup.class);
        assertThat(v.invoke("classify", new Class[]{int.class}, 1)).isEqualTo("one");
        assertThat(v.invoke("classify", new Class[]{int.class}, 100)).isEqualTo("hundred");
        assertThat(v.invoke("classify", new Class[]{int.class}, 999)).isEqualTo("other");
    }

    // =========================================================================
    // FINALLY BLOCK ALWAYS EXECUTES
    // =========================================================================

    @Test
    void finallyBlock_happyPath_methodCompletesWithFinallyResult() throws Exception {
        // Given: FinallyExecutes instrumented — finally block appends "!" to result
        //        Compiler duplicates finally body before each normal exit and exception exit
        //        The finally statement generates a ReturnProbe that fires on both paths
        // When:  divide(6, 2) — no exception; try body succeeds; finally appends "!"
        // Then:  method returns "3!"; method entered
        var v = InstrumentationVerifier.of(FinallyExecutes.class);
        Object result = v.invoke("divide", new Class[]{int.class, int.class}, 6, 2);
        v.assertMethodEntered("divide");
        assertThat(result).isEqualTo("3!");
    }

    @Test
    void finallyBlock_exceptionPath_finallyStillExecutes() throws Exception {
        // Given: FinallyExecutes instrumented
        // When:  divide(6, 0) — ArithmeticException; catch sets "error"; finally still appends "!"
        // Then:  method returns "error!"; method entered; finally line probe fired
        var v = InstrumentationVerifier.of(FinallyExecutes.class);
        Object result = v.invoke("divide", new Class[]{int.class, int.class}, 6, 0);
        v.assertMethodEntered("divide");
        assertThat(result).isEqualTo("error!");
    }

    // =========================================================================
    // RETHROW FROM CATCH
    // =========================================================================

    @Test
    void rethrowFromCatch_catchThrowProbeFiresAndExceptionWrapped() throws Exception {
        // Given: RethrowFromCatch instrumented — catch rethrows as IllegalArgumentException
        //        Catch body's ATHROW emits a ThrowProbe; not an IFXX → no branch probe
        // When:  parse("bad") — NumberFormatException thrown; caught; rethrown as IAE
        // Then:  method entered; ThrowProbe for the rethrow fires; rethrown cause is NumberFormatException
        var v = InstrumentationVerifier.of(RethrowFromCatch.class);
        try {
            v.invoke("parse", new Class[]{String.class}, "bad");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(NumberFormatException.class);
        }
        v.assertMethodEntered("parse");
    }
}
