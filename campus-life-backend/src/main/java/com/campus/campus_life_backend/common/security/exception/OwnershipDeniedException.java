package com.campus.campus_life_backend.common.security.exception;

public class OwnershipDeniedException extends PermissionDeniedException {

    public OwnershipDeniedException() {
        super("无权访问该资源");
    }

    public OwnershipDeniedException(String message) {
        super(message);
    }
}
