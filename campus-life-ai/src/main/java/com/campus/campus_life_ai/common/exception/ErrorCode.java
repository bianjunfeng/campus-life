package com.campus.campus_life_ai.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    int getCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
