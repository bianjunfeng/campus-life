package com.campus.campus_life_backend.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    int getCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
