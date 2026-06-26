package org.example.branches;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link BranchFixtures}.
 *
 * <p>Each nested class exercises one branch-condition kind. Every test pair
 * drives the branch TRUE in one test and FALSE in the other, so the generated
 * coverage report demonstrates both directions for every operand case.</p>
 */
class BranchFixturesTest {

    // -------------------------------------------------------------------
    // METHOD_CALL operands
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("checkStartsWithDot – single-arg receiver capture")
    class CheckStartsWithDot {

        @Test
        @DisplayName("dot-prefixed name → TRUE direction")
        void testDotName() {
            assertEquals("dot", BranchFixtures.checkStartsWithDot(".env"));
        }

        @Test
        @DisplayName("plain name → FALSE direction")
        void testPlainName() {
            assertEquals("plain", BranchFixtures.checkStartsWithDot("readme"));
        }
    }

    @Nested
    @DisplayName("checkEquals – receiver + argument capture")
    class CheckEquals {

        @Test
        @DisplayName("equal strings → TRUE direction")
        void testEqual() {
            assertEquals("equal", BranchFixtures.checkEquals("x", "x"));
        }

        @Test
        @DisplayName("different strings → FALSE direction")
        void testNotEqual() {
            assertEquals("different", BranchFixtures.checkEquals("x", "y"));
        }
    }

    @Nested
    @DisplayName("checkIsBlank – no-arg method call, receiver captured")
    class CheckIsBlank {

        @Test
        @DisplayName("blank string → TRUE direction")
        void testBlank() {
            assertEquals("blank", BranchFixtures.checkIsBlank(""));
        }

        @Test
        @DisplayName("non-blank string → FALSE direction")
        void testNonBlank() {
            assertEquals("non-blank", BranchFixtures.checkIsBlank("abc"));
        }
    }

    @Nested
    @DisplayName("checkAbsGreaterThanTen – static-method scope dropped, argument captured")
    class CheckAbsGreaterThanTen {

        @Test
        @DisplayName("|value| > 10 → TRUE direction")
        void testNegative() {
            assertEquals("big", BranchFixtures.checkAbsGreaterThanTen(-15));
        }

        @Test
        @DisplayName("|value| > 10 → TRUE direction")
        void testBig() {
            assertEquals("big", BranchFixtures.checkAbsGreaterThanTen(15));
        }

        @Test
        @DisplayName("|value| <= 10 → FALSE direction")
        void testSmall() {
            assertEquals("small", BranchFixtures.checkAbsGreaterThanTen(3));
        }
    }

    @Nested
    @DisplayName("checkChainedStartsWith – non-simple receiver dropped, no columns")
    class CheckChainedStartsWith {

        @Test
        @DisplayName("chained receiver starts with dot → TRUE direction")
        void testDotChained() {
            assertEquals("dot-name", BranchFixtures.checkChainedStartsWith(true));
        }

        @Test
        @DisplayName("chained receiver does not start with dot → FALSE direction")
        void testPlainChained() {
            assertEquals("plain-name", BranchFixtures.checkChainedStartsWith(false));
        }
    }

    // -------------------------------------------------------------------
    // BINARY_COMPARE operands
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("checkGreaterThanFive – lhs variable, rhs literal")
    class CheckGreaterThanFive {

        @Test
        @DisplayName("x > 5 → TRUE direction")
        void testBig() {
            assertEquals("big", BranchFixtures.checkGreaterThanFive(10));
        }

        @Test
        @DisplayName("x <= 5 → FALSE direction")
        void testSmall() {
            assertEquals("small", BranchFixtures.checkGreaterThanFive(3));
        }
    }

    @Nested
    @DisplayName("checkLiteralGreaterThanX – lhs literal, rhs variable")
    class CheckLiteralGreaterThanX {

        @Test
        @DisplayName("5 > x (x=3) → TRUE direction")
        void testLiteralWins() {
            assertEquals("big", BranchFixtures.checkLiteralGreaterThanX(3));
        }

        @Test
        @DisplayName("5 > x (x=10) is false → FALSE direction")
        void testXWins() {
            assertEquals("small", BranchFixtures.checkLiteralGreaterThanX(10));
        }
    }

    @Nested
    @DisplayName("checkAGreaterThanB – both sides variable")
    class CheckAGreaterThanB {

        @Test
        @DisplayName("a > b → TRUE direction")
        void testAWins() {
            assertEquals("a-wins", BranchFixtures.checkAGreaterThanB(5, 3));
        }

        @Test
        @DisplayName("a <= b → FALSE direction")
        void testBWins() {
            assertEquals("b-wins", BranchFixtures.checkAGreaterThanB(3, 5));
        }
    }

    @Nested
    @DisplayName("checkNullRight – null check, variable on left")
    class CheckNullRight {

        @Test
        @DisplayName("null input → TRUE direction")
        void testNullInput() {
            assertEquals("null", BranchFixtures.checkNullRight(null));
        }

        @Test
        @DisplayName("non-null input → FALSE direction")
        void testNonNullInput() {
            assertEquals("non-null", BranchFixtures.checkNullRight("value"));
        }
    }

    @Nested
    @DisplayName("checkNullLeft – null literal on left, variable on right")
    class CheckNullLeft {

        @Test
        @DisplayName("null input → TRUE direction")
        void testNullInput() {
            assertEquals("null", BranchFixtures.checkNullLeft(null));
        }

        @Test
        @DisplayName("non-null input → FALSE direction")
        void testNonNullInput() {
            assertEquals("non-null", BranchFixtures.checkNullLeft("value"));
        }
    }

    @Nested
    @DisplayName("checkReferenceEquality – both sides variable")
    class CheckReferenceEquality {

        @Test
        @DisplayName("same reference → TRUE direction")
        void testSameRef() {
            Object obj = new Object();
            assertEquals("same", BranchFixtures.checkReferenceEquality(obj, obj));
        }

        @Test
        @DisplayName("different references → FALSE direction")
        void testDifferentRef() {
            assertEquals("different",
                    BranchFixtures.checkReferenceEquality(new Object(), new Object()));
        }
    }

    // -------------------------------------------------------------------
    // Compound conditions
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("checkNullOrBlank – OR short-circuit")
    class CheckNullOrBlank {

        @Test
        @DisplayName("null → first operand TRUE")
        void testNull() {
            assertEquals("empty", BranchFixtures.checkNullOrBlank(null));
        }

        @Test
        @DisplayName("blank string → first operand FALSE, second operand TRUE")
        void testBlank() {
            assertEquals("empty", BranchFixtures.checkNullOrBlank("  "));
        }

        @Test
        @DisplayName("non-blank string → both operands FALSE")
        void testNonBlank() {
            assertEquals("non-empty", BranchFixtures.checkNullOrBlank("hello"));
        }
    }

    @Nested
    @DisplayName("checkInRange – AND short-circuit")
    class CheckInRange {

        @Test
        @DisplayName("x in (0, 100) → both operands TRUE")
        void testInRange() {
            assertEquals("in-range", BranchFixtures.checkInRange(50));
        }

        @Test
        @DisplayName("x = 0 → first operand FALSE")
        void testAtLowerBound() {
            assertEquals("out-of-range", BranchFixtures.checkInRange(0));
        }

        @Test
        @DisplayName("x = 100 → second operand FALSE")
        void testAtUpperBound() {
            assertEquals("out-of-range", BranchFixtures.checkInRange(100));
        }
    }

    // -------------------------------------------------------------------
    // Deliberately skipped operands
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("checkBareBoolean – bare boolean, no columns")
    class CheckBareBoolean {

        @Test
        @DisplayName("flag=true → TRUE direction")
        void testFlagTrue() {
            assertEquals("on", BranchFixtures.checkBareBoolean(true));
        }

        @Test
        @DisplayName("flag=false → FALSE direction")
        void testFlagFalse() {
            assertEquals("off", BranchFixtures.checkBareBoolean(false));
        }
    }

    @Nested
    @DisplayName("checkNegatedIsEmpty – unary negation of method call, no columns")
    class CheckNegatedIsEmpty {

        @Test
        @DisplayName("non-empty list → TRUE direction")
        void testNonEmpty() {
            assertEquals("non-empty", BranchFixtures.checkNegatedIsEmpty(List.of("a")));
        }

        @Test
        @DisplayName("empty list → FALSE direction")
        void testEmpty() {
            assertEquals("empty", BranchFixtures.checkNegatedIsEmpty(List.of()));
        }
    }

    @Nested
    @DisplayName("checkContainsValues – contains values in different collections")
    class CheckCollectionContainsValues {

        @Test
        @DisplayName("contains value in list → TRUE direction")
        void testContainsInList() {
            assertEquals("contains", BranchFixtures.checkContainsValues(List.of("a"), "a"));
        }

        @Test
        @DisplayName("contains value in list → TRUE direction")
        void testDoesNotContainInList() {
            assertEquals("does-not-contain", BranchFixtures.checkContainsValues(List.of("a"), "b"));
        }

        @Test
        @DisplayName("contains value in set → TRUE direction")
        void testContainsInSet() {
            assertEquals("contains", BranchFixtures.checkContainsValues(Set.of("c"), "c"));
        }

        @Test
        @DisplayName("contains value in set → TRUE direction")
        void testDoesNotContainInSet() {
            assertEquals("does-not-contain", BranchFixtures.checkContainsValues(Set.of("c"), "d"));
        }
    }
}
