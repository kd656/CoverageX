package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestedClassFixturesTest {

    @Test
    void outerClassMethodHandlesBlankAndNamedValues() {
        NestedClassFixtures fixtures = new NestedClassFixtures();

        assertEquals("unknown", fixtures.normalize(null));
        assertEquals("unknown", fixtures.normalize(" "));
        assertEquals("Alice", fixtures.normalize(" Alice "));
    }

    @Test
    void nestedClassMethodHandlesPositiveAndNonPositiveValues() {
        NestedClassFixtures.Multiplier multiplier = new NestedClassFixtures.Multiplier();

        assertEquals(8, multiplier.doubleIfPositive(4));
        assertEquals(0, multiplier.doubleIfPositive(0));
    }
}
