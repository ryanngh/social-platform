package com.ryan.socialplatform.relationship.exceptions;

import com.ryan.socialplatform.common.exception.ResourceNotFoundException;

public class FriendRequestNotFoundException extends ResourceNotFoundException {
    public FriendRequestNotFoundException() {
        super("Friend request not found");
    }

    public FriendRequestNotFoundException(String message) {
        super(message);
    }
}
