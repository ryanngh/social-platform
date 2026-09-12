package com.ryan.socialplatform.storage.enums;

/**
 * Whitelist các thư mục upload được phép trên MinIO.
 * Mỗi giá trị tương ứng với một prefix path trong bucket.
 */
public enum MediaFolder {
    POSTS("posts"),
    COMMENTS("comments"),
    MESSAGES("messages");

    private final String value;

    MediaFolder(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
