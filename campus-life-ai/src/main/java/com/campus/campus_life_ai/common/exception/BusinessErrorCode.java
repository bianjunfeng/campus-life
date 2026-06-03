package com.campus.campus_life_ai.common.exception;

import org.springframework.http.HttpStatus;

public enum BusinessErrorCode implements ErrorCode {
    INVALID_PARAM(400001, "请求参数不合法", HttpStatus.BAD_REQUEST),
    LOGIN_REQUIRED(401001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403001, "无权限访问", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(404001, "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(409001, "请求状态冲突", HttpStatus.CONFLICT),
    NOT_IMPLEMENTED(501001, "能力尚未启用", HttpStatus.NOT_IMPLEMENTED),
    INTERNAL_ERROR(500002, "AI 服务内部错误，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    BusinessErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
