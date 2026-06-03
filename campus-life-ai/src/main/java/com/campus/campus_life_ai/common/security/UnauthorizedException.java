package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.exception.BusinessErrorCode;
import com.campus.campus_life_ai.common.exception.BusinessException;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {
        super(BusinessErrorCode.LOGIN_REQUIRED, message);
    }
}
