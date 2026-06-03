package com.campus.campus_life_ai.ai.gateway;

import com.campus.campus_life_ai.ai.gateway.dto.GatewayError;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.SocketTimeoutException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Component
public class GatewayErrorNormalizer {

    public GatewayError normalize(Throwable throwable) {
        GatewayCallException gatewayCallException = findGatewayCallException(throwable);
        if (gatewayCallException != null && gatewayCallException.getError() != null) {
            return gatewayCallException.getError();
        }
        Throwable cause = rootCause(throwable);
        if (isClientAbort(throwable)) {
            return error(GatewayErrorCode.CLIENT_ABORTED, "客户端已中断流式响应");
        }
        if (isTimeout(cause)) {
            return error(GatewayErrorCode.PROVIDER_TIMEOUT, message(cause, "模型服务调用超时"));
        }
        if (isProviderUnavailable(cause)) {
            return error(GatewayErrorCode.PROVIDER_UNAVAILABLE, message(cause, "模型供应商未启用或缺少配置"));
        }
        return error(GatewayErrorCode.PROVIDER_ERROR, message(cause, "模型服务调用失败"));
    }

    public boolean isClientAborted(GatewayError error) {
        return error != null && GatewayErrorCode.CLIENT_ABORTED.getCode().equals(error.getErrorCode());
    }

    private GatewayError error(GatewayErrorCode errorCode, String message) {
        return GatewayError.builder()
                .errorCode(errorCode.getCode())
                .errorType(errorCode.getType())
                .message(message)
                .build();
    }

    private boolean isProviderUnavailable(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return StringUtils.hasText(message)
                && (message.contains("供应商未启用")
                || message.contains("缺少配置")
                || message.contains("未启用可用的大模型供应商配置"));
    }

    private boolean isTimeout(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException) {
            return true;
        }
        String className = throwable == null ? "" : throwable.getClass().getName().toLowerCase(Locale.ROOT);
        String message = throwable == null || throwable.getMessage() == null
                ? ""
                : throwable.getMessage().toLowerCase(Locale.ROOT);
        return className.contains("timeout") || message.contains("timed out") || message.contains("timeout");
    }

    private boolean isClientAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getClass().getSimpleName().contains("ClientStreamClosedException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private GatewayCallException findGatewayCallException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof GatewayCallException gatewayCallException) {
                return gatewayCallException;
            }
            current = current.getCause();
        }
        return null;
    }

    private String message(Throwable throwable, String fallback) {
        return throwable != null && StringUtils.hasText(throwable.getMessage())
                ? throwable.getMessage()
                : fallback;
    }
}
