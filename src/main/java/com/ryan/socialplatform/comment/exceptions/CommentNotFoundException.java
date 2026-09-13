package com.ryan.socialplatform.comment.exceptions;

import com.ryan.socialplatform.common.exception.ResourceNotFoundException;

public class CommentNotFoundException extends ResourceNotFoundException {
    public CommentNotFoundException() {
        super("Comment not found");
    }

    public CommentNotFoundException(String message) {
        super(message);
    }
}
