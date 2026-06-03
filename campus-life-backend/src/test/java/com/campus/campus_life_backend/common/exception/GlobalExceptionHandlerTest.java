package com.campus.campus_life_backend.common.exception;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.exception.PermissionDeniedException;
import com.campus.campus_life_backend.common.security.exception.UnauthenticatedException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBusinessErrorCodeWhenBusinessException() {
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(
                new BusinessException(BusinessErrorCode.FORBIDDEN, "无权限访问")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(BusinessErrorCode.FORBIDDEN.getCode(), response.getBody().getCode());
        assertEquals("无权限访问", response.getBody().getMessage());
    }

    @Test
    void shouldHideRuntimeExceptionDetailsForInternalServerError() {
        ResponseEntity<ApiResponse<?>> response = handler.handleRuntimeException(new RuntimeException("jdbc:mysql://root:secret@localhost"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getMessage(), response.getBody().getMessage());
    }

    @Test
    void shouldReturn400ForVoucherBusinessException() {
        ResponseEntity<ApiResponse<?>> response = handler.handleBusinessException(
                new BusinessException(BusinessErrorCode.VOUCHER_DUPLICATE_ORDER)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(BusinessErrorCode.VOUCHER_DUPLICATE_ORDER.getCode(), response.getBody().getCode());
        assertEquals("该优惠券已有有效订单，请勿重复下单", response.getBody().getMessage());
    }

    @Test
    void shouldHideDataAccessExceptionDetails() {
        DataAccessException exception = new DataAccessException("Unknown column 'password_hash'") {};

        ResponseEntity<ApiResponse<?>> response = handler.handleDataAccessException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BusinessErrorCode.DATA_ACCESS_ERROR.getCode(), response.getBody().getCode());
        assertEquals(BusinessErrorCode.DATA_ACCESS_ERROR.getMessage(), response.getBody().getMessage());
    }

    @Test
    void shouldReturn401ForAuthenticationException() {
        AuthenticationException exception = new AuthenticationException("token invalid") {};

        ResponseEntity<ApiResponse<?>> response = handler.handleAuthenticationException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(BusinessErrorCode.LOGIN_REQUIRED.getCode(), response.getBody().getCode());
        assertEquals("未登录或登录已过期", response.getBody().getMessage());
    }

    @Test
    void shouldReturn401ForUnauthenticatedException() {
        ResponseEntity<ApiResponse<?>> response = handler.handleUnauthenticatedException(new UnauthenticatedException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(BusinessErrorCode.LOGIN_REQUIRED.getCode(), response.getBody().getCode());
        assertEquals("未登录或登录已过期", response.getBody().getMessage());
    }

    @Test
    void shouldReturn403ForPermissionDeniedException() {
        ResponseEntity<ApiResponse<?>> response = handler.handlePermissionDeniedException(
                new PermissionDeniedException("缺少角色权限: ADMIN")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(BusinessErrorCode.FORBIDDEN.getCode(), response.getBody().getCode());
        assertEquals("缺少角色权限: ADMIN", response.getBody().getMessage());
    }

    @Test
    void shouldReturn405ForUnsupportedMethod() {
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("GET");

        ResponseEntity<ApiResponse<?>> response = handler.handleMethodNotSupportedException(exception);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(BusinessErrorCode.METHOD_NOT_ALLOWED.getCode(), response.getBody().getCode());
        assertEquals("请求方法不支持: GET", response.getBody().getMessage());
    }

    @Test
    void shouldHideUnhandledExceptionDetails() {
        ResponseEntity<ApiResponse<?>> response = handler.handleException(new Exception("redis password leaked"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
        assertEquals(BusinessErrorCode.INTERNAL_ERROR.getMessage(), response.getBody().getMessage());
    }
}
