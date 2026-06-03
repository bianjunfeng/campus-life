package com.campus.campus_life_backend.common.security.exception;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;

public class PermissionDeniedException extends BusinessException {

    public PermissionDeniedException() {
        super(BusinessErrorCode.FORBIDDEN);
    }

    public PermissionDeniedException(String message) {
        super(BusinessErrorCode.FORBIDDEN, message);
    }
}
