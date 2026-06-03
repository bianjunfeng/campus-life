package com.campus.campus_life_ai.common.security;

import com.campus.campus_life_ai.common.exception.BusinessErrorCode;
import com.campus.campus_life_ai.common.exception.BusinessException;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(BusinessErrorCode.FORBIDDEN, message);
    }
}
