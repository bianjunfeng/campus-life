package com.campus.campus_life_backend.common.security.exception;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;

public class UnauthenticatedException extends BusinessException {

    public UnauthenticatedException() {
        super(BusinessErrorCode.LOGIN_REQUIRED);
    }

    public UnauthenticatedException(String message) {
        super(BusinessErrorCode.LOGIN_REQUIRED, message);
    }
}
