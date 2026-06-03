package com.campus.campus_life_backend.common.security.model;

public enum PermissionCode {
    ADMIN_ACCESS("admin:access"),
    ADMIN_POST_MANAGE("admin:post:manage"),
    ADMIN_USER_MANAGE("admin:user:manage"),
    ADMIN_SEARCH_VIEW("admin:search:view"),
    ADMIN_SEARCH_REINDEX("admin:search:reindex"),
    ADMIN_REFUND_REVIEW("admin:refund:review"),
    ADMIN_PAYMENT_RECONCILE("admin:payment:reconcile"),
    MERCHANT_ACCESS("merchant:access"),
    MERCHANT_PROFILE_MANAGE("merchant:profile:manage:self"),
    MERCHANT_LOCATION_UPDATE("merchant:location:update:self"),
    MERCHANT_VOUCHER_MANAGE("merchant:voucher:manage:self"),
    MERCHANT_REFUND_REVIEW("merchant:refund:review:self"),
    USER_PROFILE_READ_SELF("user:profile:read:self"),
    USER_PROFILE_UPDATE_SELF("user:profile:update:self"),
    POST_CREATE("post:create"),
    POST_UPDATE_SELF("post:update:self"),
    POST_DELETE_SELF("post:delete:self"),
    COMMENT_CREATE("comment:create"),
    COMMENT_DELETE_SELF("comment:delete:self"),
    REPORT_CREATE("report:create"),
    MESSAGE_USE("message:use"),
    FILE_UPLOAD("file:upload"),
    ORDER_READ_SELF("order:read:self"),
    PAYMENT_REFUND_APPLY_SELF("payment:refund:apply:self"),
    VOUCHER_ORDER_SELF("voucher:order:self"),
    AI_USE("ai:use");

    private static final String AUTHORITY_PREFIX = "PERM_";

    private final String code;

    PermissionCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public String authority() {
        return toAuthority(code);
    }

    public static String toAuthority(String code) {
        return AUTHORITY_PREFIX + code;
    }
}
