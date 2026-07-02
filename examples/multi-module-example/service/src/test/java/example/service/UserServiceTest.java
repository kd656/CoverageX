package example.service;

import example.dto.UserRecord;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {

    @Test
    void adultUserGetsWelcomeMessage() {
        UserService service = new UserService();
        assertEquals(
            "Hello, Alice — welcome.",
            service.describe(new UserRecord("Alice", 30)));
    }

    @Test
    void minorUserRequiresParentalConsent() {
        UserService service = new UserService();
        assertEquals(
            "Hello, Bob — parental consent required.",
            service.describe(new UserRecord("Bob", 12)));
    }
}
