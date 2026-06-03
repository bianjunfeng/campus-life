package com.campus.campus_life_ai.common.config;

import com.campus.campus_life_ai.common.exception.BusinessErrorCode;
import com.campus.campus_life_ai.common.exception.BusinessException;
import com.campus.campus_life_ai.common.security.ForbiddenException;
import com.campus.campus_life_ai.common.security.UnauthorizedException;
import com.campus.campus_life_ai.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("AI 请求参数错误: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(BusinessErrorCode.INVALID_PARAM.getCode(), e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn("AI 请求未授权: {}", e.getMessage());
        return ResponseEntity.status(401).body(ApiResponse.error(BusinessErrorCode.LOGIN_REQUIRED.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e) {
        log.warn("AI 请求权限不足: {}", e.getMessage());
        return ResponseEntity.status(403).body(ApiResponse.error(BusinessErrorCode.FORBIDDEN.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(BusinessErrorCode.INVALID_PARAM.getMessage());
        log.warn("AI 请求校验失败: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(BusinessErrorCode.INVALID_PARAM.getCode(), message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        log.error("AI 运行状态异常", e);
        return ResponseEntity.status(409).body(ApiResponse.error(BusinessErrorCode.CONFLICT.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        log.error("AI 服务发生未处理异常", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error(BusinessErrorCode.INTERNAL_ERROR.getCode(),
                BusinessErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
