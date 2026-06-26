package org.example.branches;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BranchCaptureShapes}.
 *
 * <p>Each nested class exercises one fixture method, driving both the TRUE and FALSE
 * branch directions so that the generated coverage report demonstrates complete
 * two-way coverage for every supported capture shape.</p>
 *
 * <p>These tests also serve as the end-to-end smoke check for the capture shapes
 * introduced in PR 2: the CoverageX agent instruments the code at test-execution time,
 * and the generated HTML report should show:</p>
 * <ul>
 *   <li>Per-operand columns with captured values for instance and static calls.</li>
 *   <li>Category-2 (long/double) arguments boxed correctly as {@code Long}/{@code Double}.</li>
 *   <li>Nested-call fixtures showing the <em>outer</em> call's arguments, not the
 *       inner call's.</li>
 *   <li>{@code longLiteralCompare} with <em>no</em> operand column (the cat-2 filter).</li>
 * </ul>
 */
class BranchCaptureShapesTest {

    // -----------------------------------------------------------------------
    // rangeContains — instance call, two non-literal int args
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("rangeContains – receiver + two int args captured")
    class RangeContains {

        @Test
        @DisplayName("range contains bounds → TRUE direction")
        void testContains() {
            Range range = new Range(0, 100);
            assertTrue(BranchCaptureShapes.rangeContains(range, 10, 90));
        }

        @Test
        @DisplayName("range does not contain bounds → FALSE direction")
        void testNotContains() {
            Range range = new Range(0, 50);
            assertFalse(BranchCaptureShapes.rangeContains(range, 10, 90));
        }
    }

    // -----------------------------------------------------------------------
    // classifyStatic — static call, one non-literal arg, no receiver column
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("classifyStatic – static call, arg captured, no receiver column")
    class ClassifyStatic {

        @Test
        @DisplayName("blank name → TRUE direction")
        void testBlank() {
            assertTrue(BranchCaptureShapes.classifyStatic(""));
        }

        @Test
        @DisplayName("non-blank name → FALSE direction")
        void testNonBlank() {
            assertFalse(BranchCaptureShapes.classifyStatic("hello"));
        }
    }

    // -----------------------------------------------------------------------
    // rulesMatch — static call, two non-literal args
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("rulesMatch – static call with two args captured")
    class RulesMatch {

        @Test
        @DisplayName("matching strings → TRUE direction")
        void testMatches() {
            assertTrue(BranchCaptureShapes.rulesMatch("alpha", "alpha"));
        }

        @Test
        @DisplayName("non-matching strings → FALSE direction")
        void testNoMatch() {
            assertFalse(BranchCaptureShapes.rulesMatch("alpha", "beta"));
        }
    }

    // -----------------------------------------------------------------------
    // longCheck — category-2 long arg
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("longCheck – long arg spilled as category-2")
    class LongCheck {

        @Test
        @DisplayName("value within limits → TRUE direction")
        void testAllowed() {
            assertTrue(BranchCaptureShapes.longCheck(new Limits(200L), 100L));
        }

        @Test
        @DisplayName("value exceeds limits → FALSE direction")
        void testDenied() {
            assertFalse(BranchCaptureShapes.longCheck(new Limits(50L), 100L));
        }
    }

    // -----------------------------------------------------------------------
    // doubleCheck — category-2 double arg
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("doubleCheck – double arg spilled as category-2")
    class DoubleCheck {

        @Test
        @DisplayName("score above threshold → TRUE direction")
        void testAccepted() {
            assertTrue(BranchCaptureShapes.doubleCheck(new Parser(1.0), 2.5));
        }

        @Test
        @DisplayName("score below threshold → FALSE direction")
        void testRejected() {
            assertFalse(BranchCaptureShapes.doubleCheck(new Parser(3.0), 1.5));
        }
    }

    // -----------------------------------------------------------------------
    // mixedCheck — mixed category-1 + category-2
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("mixedCheck – long offset + double score, all three captured")
    class MixedCheck {

        @Test
        @DisplayName("non-negative offset and sufficient score → TRUE direction")
        void testAccepted() {
            assertTrue(BranchCaptureShapes.mixedCheck(new Parser(1.0), 10L, 2.5));
        }

        @Test
        @DisplayName("negative offset → FALSE direction")
        void testNegativeOffset() {
            assertFalse(BranchCaptureShapes.mixedCheck(new Parser(1.0), -1L, 2.5));
        }

        @Test
        @DisplayName("score below threshold → FALSE direction")
        void testLowScore() {
            assertFalse(BranchCaptureShapes.mixedCheck(new Parser(3.0), 0L, 1.5));
        }
    }

    // -----------------------------------------------------------------------
    // nestedDifferentName — nested call, outer call captured
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("nestedDifferentName – outer canAccess() captured, inner resolveUser() skipped")
    class NestedDifferentName {

        @Test
        @DisplayName("admin role → TRUE direction")
        void testAdmin() {
            assertTrue(BranchCaptureShapes.nestedDifferentName(new Service(), "admin"));
        }

        @Test
        @DisplayName("non-admin role → FALSE direction")
        void testNonAdmin() {
            assertFalse(BranchCaptureShapes.nestedDifferentName(new Service(), "viewer"));
        }
    }

    // -----------------------------------------------------------------------
    // nestedBooleanInner — boolean-returning inner call, outer captured via name guard
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("nestedBooleanInner – outer accepts() captured; inner flag() skipped by name guard")
    class NestedBooleanInner {

        @Test
        @DisplayName("flag=true and non-blank role → TRUE direction")
        void testAccepted() {
            assertTrue(BranchCaptureShapes.nestedBooleanInner(
                    new Rules(), new FlagProvider(true), "admin"));
        }

        @Test
        @DisplayName("flag=false → FALSE direction")
        void testFlagFalse() {
            assertFalse(BranchCaptureShapes.nestedBooleanInner(
                    new Rules(), new FlagProvider(false), "admin"));
        }

        @Test
        @DisplayName("blank role → FALSE direction")
        void testBlankRole() {
            assertFalse(BranchCaptureShapes.nestedBooleanInner(
                    new Rules(), new FlagProvider(true), ""));
        }
    }

    // -----------------------------------------------------------------------
    // longLiteralCompare — cat-2 filter; no operand column in report
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("longLiteralCompare – cat-2 analyser filter; popover has no operand column")
    class LongLiteralCompare {

        @Test
        @DisplayName("x > 5L (x=10) → TRUE direction")
        void testTrue() {
            assertTrue(BranchCaptureShapes.longLiteralCompare(10L));
        }

        @Test
        @DisplayName("x > 5L (x=3) → FALSE direction")
        void testFalse() {
            assertFalse(BranchCaptureShapes.longLiteralCompare(3L));
        }
    }
}
