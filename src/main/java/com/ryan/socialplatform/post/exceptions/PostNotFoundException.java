package com.ryan.socialplatform.post.exceptions;

import com.ryan.socialplatform.common.exception.ResourceNotFoundException;

public class PostNotFoundException extends ResourceNotFoundException {
    public PostNotFoundException() {
        super("Post not found");
    }

    public PostNotFoundException(String message) {
        super(message);
    }
}
