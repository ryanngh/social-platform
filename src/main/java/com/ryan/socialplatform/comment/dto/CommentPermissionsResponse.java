package com.ryan.socialplatform.comment.dto;

public record CommentPermissionsResponse(
        boolean canEdit,
        boolean canDelete,
        boolean canPin
) {
    public static CommentPermissionsResponse of(boolean canEdit, boolean canDelete, boolean canPin) {
        return new CommentPermissionsResponse(canEdit, canDelete, canPin);
    }
}
