package com.ryan.socialplatform.user.exceptions;

import com.ryan.socialplatform.common.exception.ConflictException;

public class AccountStatusException extends ConflictException {
    public AccountStatusException(String message) {
        super(message);
    }
}
