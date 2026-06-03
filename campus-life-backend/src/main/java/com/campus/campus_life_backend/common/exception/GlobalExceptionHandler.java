package com.campus.campus_life_backend.common.exception;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.exception.PermissionDeniedException;
import com.campus.campus_life_backend.common.security.exception.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BusinessErrorCode.INTERNAL_ERROR.getCode(),
                        BusinessErrorCode.INTERNAL_ERROR.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldError() == null
                ? BusinessErrorCode.INVALID_PARAM.getMessage()
                : e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessErrorCode.INVALID_PARAM.getCode(), errorMessage));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? BusinessErrorCode.INVALID_PARAM.getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessErrorCode.INVALID_PARAM.getCode(), message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalStateException(IllegalStateException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? BusinessErrorCode.CONFLICT.getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(BusinessErrorCode.CONFLICT.getCode(), message));
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthenticatedException(UnauthenticatedException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? BusinessErrorCode.LOGIN_REQUIRED.getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(BusinessErrorCode.LOGIN_REQUIRED.getCode(), message));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handlePermissionDeniedException(PermissionDeniedException e) {
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? BusinessErrorCode.FORBIDDEN.getMessage()
                : e.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(BusinessErrorCode.FORBIDDEN.getCode(), message));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(BusinessErrorCode.LOGIN_REQUIRED.getCode(),
                        BusinessErrorCode.LOGIN_REQUIRED.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(BusinessErrorCode.FORBIDDEN.getCode(),
                        BusinessErrorCode.FORBIDDEN.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String method = e.getMethod() != null ? e.getMethod() : "UNKNOWN";
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(BusinessErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        BusinessErrorCode.METHOD_NOT_ALLOWED.getMessage() + ": " + method));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleDataAccessException(DataAccessException e) {
        logger.error("数据库访问异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BusinessErrorCode.DATA_ACCESS_ERROR.getCode(),
                        BusinessErrorCode.DATA_ACCESS_ERROR.getMessage()));
    }

    /**
     * 处理静态资源未找到的异常（如 favicon.ico、根路径等）
     * 这些是浏览器自动请求的，不需要记录为错误
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        String resourcePath = e.getResourcePath();
        // 对静态资源请求和常见的无效API路径降级为DEBUG
        if (resourcePath != null && (
            resourcePath.equals("favicon.ico") ||
            resourcePath.equals(".") ||
            resourcePath.isEmpty() ||
            resourcePath.equals("api") ||
            resourcePath.startsWith("api/") ||
            resourcePath.endsWith(".ico") ||
            resourcePath.endsWith(".png") ||
            resourcePath.endsWith(".jpg") ||
            resourcePath.endsWith(".gif") ||
            resourcePath.endsWith(".css") ||
            resourcePath.endsWith(".js") ||
            resourcePath.endsWith(".svg")
        )) {
            logger.debug("静态资源未找到: {}", resourcePath);
        } else {
            logger.warn("资源未找到: {}", resourcePath);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(BusinessErrorCode.RESOURCE_NOT_FOUND.getCode(),
                        BusinessErrorCode.RESOURCE_NOT_FOUND.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        logger.error("未处理的异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BusinessErrorCode.INTERNAL_ERROR.getCode(),
                        BusinessErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
