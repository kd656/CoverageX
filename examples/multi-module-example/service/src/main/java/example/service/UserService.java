package example.service;

import example.dto.UserRecord;

public final class UserService {

    public String describe(UserRecord user) {
        if (user.isAdult()) {
            return user.greeting() + " — welcome.";
        }
        return user.greeting() + " — parental consent required.";
    }
}
