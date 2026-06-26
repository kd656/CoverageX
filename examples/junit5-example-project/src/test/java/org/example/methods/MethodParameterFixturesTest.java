package org.example.methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link MethodParameterFixtures}.
 *
 * <p>Each test calls the fixture with distinct {@code (name, threshold)} or
 * {@code (prefix, subject)} combinations so the generated coverage report
 * demonstrates per-parameter columns in the method-invocation popover.
 * The popover should show {@code TEST METHOD | CALLS | NAME | THRESHOLD}
 * (or {@code PREFIX | SUBJECT}) with the expected per-row values.</p>
 */
class MethodParameterFixturesTest {

    // -------------------------------------------------------------------
    // classify(String name, int threshold)
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("classify – two-parameter column rendering")
    class Classify {

        @Test
        @DisplayName("null name → \"null\" result")
        void nullName_returnsNull() {
            assertEquals("null", MethodParameterFixtures.classify(null, 5));
        }

        @Test
        @DisplayName("short name → \"short\" result")
        void shortName_belowThreshold() {
            assertEquals("short", MethodParameterFixtures.classify("hi", 5));
        }

        @Test
        @DisplayName("long name → \"long\" result")
        void longName_aboveThreshold() {
            assertEquals("long", MethodParameterFixtures.classify("extraordinarily", 5));
        }

        @Test
        @DisplayName("name equal to threshold → \"short\" result")
        void nameEqualToThreshold_returnsShort() {
            assertEquals("short", MethodParameterFixtures.classify("exact", 5));
        }
    }

    // -------------------------------------------------------------------
    // greet(String prefix, String subject)
    // -------------------------------------------------------------------

    @Nested
    @DisplayName("greet – two-parameter column rendering")
    class Greet {

        @Test
        @DisplayName("Hello World greeting")
        void helloWorld() {
            assertEquals("Hello, World!", MethodParameterFixtures.greet("Hello", "World"));
        }

        @Test
        @DisplayName("Hi CoverageX greeting")
        void hiCoverageX() {
            assertEquals("Hi, CoverageX!", MethodParameterFixtures.greet("Hi", "CoverageX"));
        }
    }
}
