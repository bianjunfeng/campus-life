package com.campus.campus_life_backend.modules.payment.dto;

/**
 * 支付回调处理结果，供 Controller 记录 callback log 与返回渠道应答。
 */
public final class PaymentCallbackResult {

    private final boolean success;
    private final boolean signatureVerified;
    private final boolean amountVerified;
    private final String errorCode;

    private PaymentCallbackResult(
            boolean success,
            boolean signatureVerified,
            boolean amountVerified,
            String errorCode
    ) {
        this.success = success;
        this.signatureVerified = signatureVerified;
        this.amountVerified = amountVerified;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSignatureVerified() {
        return signatureVerified;
    }

    public boolean isAmountVerified() {
        return amountVerified;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static PaymentCallbackResult processed() {
        return new PaymentCallbackResult(true, true, true, null);
    }

    public static PaymentCallbackResult processFailed() {
        return new PaymentCallbackResult(false, true, true, "PROCESS_CALLBACK_FAILED");
    }

    public static PaymentCallbackResult signatureInvalid() {
        return new PaymentCallbackResult(false, false, false, "SIGNATURE_INVALID");
    }

    public static PaymentCallbackResult amountMismatch() {
        return new PaymentCallbackResult(false, true, false, "AMOUNT_MISMATCH");
    }

    public static PaymentCallbackResult channelRejected(String errorCode) {
        return new PaymentCallbackResult(false, true, true, errorCode);
    }
}
