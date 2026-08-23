package com.ryan.socialplatform.user.exceptions;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID userId) {
        super("Could not find user with id: " + userId);
    }
}
