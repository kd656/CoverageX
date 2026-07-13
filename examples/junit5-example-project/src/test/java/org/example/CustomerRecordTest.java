package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerRecordTest {

    @Test
    void adultReturnsTrueForAdults() {
        CustomerRecord customer = new CustomerRecord("Alice", 21);

        assertTrue(customer.adult());
    }

    @Test
    void adultReturnsFalseForMinors() {
        CustomerRecord customer = new CustomerRecord("Bob", 16);

        assertFalse(customer.adult());
    }
}
