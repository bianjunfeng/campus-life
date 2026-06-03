package com.campus.campus_life_backend.common.exception;

import org.springframework.http.HttpStatus;

public enum BusinessErrorCode implements ErrorCode {
    INVALID_PARAM(400001, "请求参数不合法", HttpStatus.BAD_REQUEST),
    LOGIN_REQUIRED(401001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403001, "无权限访问", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(404001, "资源不存在", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405001, "请求方法不支持", HttpStatus.METHOD_NOT_ALLOWED),
    LEGACY_API_GONE(410001, "接口已迁移", HttpStatus.GONE),
    CONFLICT(409001, "请求状态冲突", HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(429001, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),
    DATA_ACCESS_ERROR(500001, "数据库操作失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(500002, "服务器内部错误，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(503001, "服务暂不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE),

    AUTH_SERVICE_UNAVAILABLE(503101, "认证服务暂不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE),
    CAPTCHA_REQUIRED(400101, "验证码不能为空", HttpStatus.BAD_REQUEST),
    CAPTCHA_INVALID(400102, "验证码错误或已过期", HttpStatus.BAD_REQUEST),
    PHONE_REGISTERED(409101, "手机号已被注册", HttpStatus.CONFLICT),
    EMAIL_REGISTERED(409102, "邮箱已被注册", HttpStatus.CONFLICT),
    USERNAME_REGISTERED(409103, "用户名已被注册", HttpStatus.CONFLICT),
    REGISTER_PROFILE_REQUIRED(400103, "请提供学生信息或商家信息", HttpStatus.BAD_REQUEST),
    REGISTER_ROLE_CONFLICT(400104, "不能同时注册学生和商家账号，请选择一种类型", HttpStatus.BAD_REQUEST),
    STUDENT_NO_REGISTERED(409104, "该学号已被注册", HttpStatus.CONFLICT),
    CONTACT_PHONE_INVALID(400105, "联系电话格式不正确", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404101, "用户不存在", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_REGISTER_FIRST(404102, "用户不存在，请先注册", HttpStatus.NOT_FOUND),
    ACCOUNT_BANNED(403101, "账号已被封禁", HttpStatus.FORBIDDEN),
    CAPTCHA_LOGIN_PHONE_ONLY(400106, "验证码登录仅支持手机号", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_SET(400107, "该账号未设置密码，请使用验证码登录或第三方登录", HttpStatus.BAD_REQUEST),
    PASSWORD_FORMAT_INVALID(400108, "密码格式错误，请联系管理员重置密码", HttpStatus.BAD_REQUEST),
    PASSWORD_WRONG(401101, "密码错误", HttpStatus.UNAUTHORIZED),
    LOGIN_CREDENTIAL_REQUIRED(400109, "请提供密码或验证码", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_INVALID(401102, "刷新令牌无效或已过期", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_TYPE_INVALID(401103, "无效的刷新令牌类型", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_USER_ID_MISSING(401104, "无法从刷新令牌中获取用户ID", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(401105, "刷新令牌已失效", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED(401106, "刷新令牌已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    OAUTH_STATE_INVALID(400110, "OAuth状态无效或已过期", HttpStatus.BAD_REQUEST),
    OAUTH_STATE_REQUIRED(400111, "OAuth state 不能为空", HttpStatus.BAD_REQUEST),
    OAUTH_CONFIG_MISSING(500101, "第三方登录配置缺失", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH_AUTHORIZE_URL_FAILED(500102, "生成第三方授权URL失败", HttpStatus.INTERNAL_SERVER_ERROR),
    OAUTH_ACCESS_TOKEN_FAILED(502101, "获取第三方AccessToken失败", HttpStatus.BAD_GATEWAY),
    OAUTH_ACCESS_TOKEN_MISSING(502102, "第三方AccessToken或OpenID缺失", HttpStatus.BAD_GATEWAY),
    OAUTH_USERINFO_FAILED(502103, "获取第三方用户信息失败", HttpStatus.BAD_GATEWAY),
    OAUTH_TOKEN_FORMAT_INVALID(502104, "第三方AccessToken格式错误", HttpStatus.BAD_GATEWAY),

    FILE_UPLOAD_FAILED(500201, "文件上传失败，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),

    MERCHANT_NAME_REQUIRED(400301, "商家名称不能为空", HttpStatus.BAD_REQUEST),
    MERCHANT_PHONE_REQUIRED(400302, "联系电话不能为空", HttpStatus.BAD_REQUEST),
    MERCHANT_NOT_FOUND(404301, "商家不存在", HttpStatus.NOT_FOUND),
    MERCHANT_CATEGORY_NOT_FOUND(404302, "商家分类不存在", HttpStatus.NOT_FOUND),
    MERCHANT_CATEGORY_REQUIRED(400303, "缺少可用商家分类，请先配置商家分类", HttpStatus.BAD_REQUEST),
    MERCHANT_AUTH_ALREADY_APPROVED_OTHER(409301, "您已通过其他认证，无需重复提交", HttpStatus.CONFLICT),
    MERCHANT_AUTH_PENDING_DUPLICATE(409302, "已有认证申请在审核中，请勿重复提交", HttpStatus.CONFLICT),
    MERCHANT_AUTH_ALREADY_APPROVED(409303, "您已通过认证，无需重复提交", HttpStatus.CONFLICT),
    MERCHANT_AUTH_STATUS_NOT_ALLOWED(409304, "当前状态不允许再次提交认证", HttpStatus.CONFLICT),
    SECKILL_VOUCHER_CREATE_FAILED(500301, "秒杀券创建失败", HttpStatus.INTERNAL_SERVER_ERROR),
    SECKILL_VOUCHER_UPDATE_FAILED(500302, "秒杀券更新失败", HttpStatus.INTERNAL_SERVER_ERROR),

    FORUM_POST_NOT_FOUND(404401, "帖子不存在", HttpStatus.NOT_FOUND),
    FORUM_COMMENT_NOT_FOUND(404402, "评论不存在", HttpStatus.NOT_FOUND),
    REPORT_STATUS_INVALID(400401, "处理状态不合法，仅支持 1-已处理 或 2-忽略", HttpStatus.BAD_REQUEST),
    REPORT_NOT_FOUND(404403, "举报不存在", HttpStatus.NOT_FOUND),
    REPORT_ALREADY_HANDLED(409401, "该举报已处理", HttpStatus.CONFLICT),
    REPORT_PROCESS_FAILED(500401, "举报处理失败", HttpStatus.INTERNAL_SERVER_ERROR),
    REPORT_TARGET_INCOMPLETE(400402, "举报目标不完整，无法执行处置", HttpStatus.BAD_REQUEST),
    REPORT_TARGET_TYPE_INVALID(400403, "举报目标类型与处置动作不匹配", HttpStatus.BAD_REQUEST),
    REPORT_ACTION_UNSUPPORTED(400404, "不支持的处置动作", HttpStatus.BAD_REQUEST),

    ORDER_NOT_FOUND(404501, "订单不存在", HttpStatus.NOT_FOUND),
    ORDER_NOT_CANCELABLE(409501, "订单不可取消", HttpStatus.CONFLICT),
    ORDER_AMOUNT_INVALID(400501, "订单金额异常", HttpStatus.BAD_REQUEST),
    ORDER_STATUS_CHANGED(409502, "订单状态已变化，请刷新后重试", HttpStatus.CONFLICT),
    ORDER_QUERY_FORBIDDEN(403501, "无权查询该订单", HttpStatus.FORBIDDEN),
    VOUCHER_STOCK_DEDUCT_FAILED(409503, "库存不足，扣减失败", HttpStatus.CONFLICT),
    VOUCHER_SALES_UPDATE_FAILED(500501, "销量更新失败", HttpStatus.INTERNAL_SERVER_ERROR),

    PAYMENT_FORBIDDEN(403601, "无权操作此订单", HttpStatus.FORBIDDEN),
    PAYMENT_RECORD_NOT_FOUND(404601, "支付单不存在", HttpStatus.NOT_FOUND),
    WALLET_BALANCE_NOT_ENOUGH(400601, "钱包余额不足", HttpStatus.BAD_REQUEST),
    PAYMENT_STATUS_CHANGED(409601, "支付单状态已变化，请刷新后重试", HttpStatus.CONFLICT),
    WECHAT_PAYMENT_DISABLED(503601, "微信支付未启用", HttpStatus.SERVICE_UNAVAILABLE),
    ALIPAY_PAYMENT_DISABLED(503602, "支付宝支付未启用", HttpStatus.SERVICE_UNAVAILABLE),
    PAYMENT_CREATE_FAILED(502601, "创建支付订单失败", HttpStatus.BAD_GATEWAY),

    VOUCHER_NOT_FOUND(404701, "优惠券不存在", HttpStatus.NOT_FOUND),
    VOUCHER_NOT_AVAILABLE(404702, "优惠券不存在或已下架", HttpStatus.NOT_FOUND),
    VOUCHER_SOLD_OUT(409701, "优惠券已售罄", HttpStatus.CONFLICT),
    VOUCHER_OFFLINE(409702, "优惠券未上架", HttpStatus.CONFLICT),
    VOUCHER_DUPLICATE_ORDER(409703, "该优惠券已有有效订单，请勿重复下单", HttpStatus.CONFLICT),
    VOUCHER_STOCK_INVALID(400701, "库存必须大于等于0", HttpStatus.BAD_REQUEST),
    VOUCHER_AMOUNT_INVALID(400702, "面值必须大于0", HttpStatus.BAD_REQUEST),
    VOUCHER_PAY_VALUE_INVALID(400703, "支付金额不能小于0", HttpStatus.BAD_REQUEST),
    VOUCHER_PAY_VALUE_EXCEEDS_AMOUNT(400704, "支付金额不能大于面值", HttpStatus.BAD_REQUEST),
    VOUCHER_TIME_RANGE_INVALID(400705, "开始时间不能晚于结束时间", HttpStatus.BAD_REQUEST),
    VOUCHER_TITLE_REQUIRED(400706, "优惠券标题不能为空", HttpStatus.BAD_REQUEST),
    NOT_SECKILL_VOUCHER(400707, "该券不是秒杀券", HttpStatus.BAD_REQUEST),
    SECKILL_BUSY(429701, "秒杀系统繁忙，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    STOCK_NOT_ENOUGH(409704, "库存不足", HttpStatus.CONFLICT),
    SECKILL_DUPLICATE(409705, "不可重复抢购", HttpStatus.CONFLICT),
    SECKILL_NOT_STARTED(400708, "秒杀未开始", HttpStatus.BAD_REQUEST),
    SECKILL_ENDED(400709, "秒杀已结束", HttpStatus.BAD_REQUEST),
    ORDER_NO_REQUIRED(400710, "订单号不能为空", HttpStatus.BAD_REQUEST),
    SECKILL_VOUCHER_NOT_FOUND(404703, "秒杀券不存在", HttpStatus.NOT_FOUND),
    SECKILL_QUEUE_FAILED(503701, "秒杀队列异常且兜底处理失败，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE),
    VOUCHER_PARAM_INVALID(400711, "优惠券参数错误", HttpStatus.BAD_REQUEST),
    CART_SECKILL_NOT_ALLOWED(400712, "秒杀券不支持加入购物车，请直接抢购下单", HttpStatus.BAD_REQUEST),
    CART_ITEM_INVALID(400713, "购物车项参数错误", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(404704, "购物车项不存在", HttpStatus.NOT_FOUND),
    CART_EMPTY(400714, "请选择要结算的商品", HttpStatus.BAD_REQUEST),
    CART_SECKILL_CHECKOUT_NOT_ALLOWED(400715, "秒杀券不支持购物车结算，请直接抢购下单", HttpStatus.BAD_REQUEST),

    STUDENT_AUTH_REQUEST_NOT_FOUND(404801, "学生认证申请不存在", HttpStatus.NOT_FOUND),
    STUDENT_AUTH_ALREADY_APPROVED_OTHER(409801, "您已通过其他认证，无需重复提交", HttpStatus.CONFLICT),
    STUDENT_AUTH_PENDING_DUPLICATE(409802, "已有认证申请在审核中，请勿重复提交", HttpStatus.CONFLICT),
    STUDENT_AUTH_ALREADY_APPROVED(409803, "您已通过认证，无需重复提交", HttpStatus.CONFLICT),
    STUDENT_AUTH_STATUS_NOT_ALLOWED(409804, "当前状态不允许再次提交认证", HttpStatus.CONFLICT),
    STUDENT_AUTH_ALREADY_PASSED(409805, "该申请已审核，不能重复通过", HttpStatus.CONFLICT),
    STUDENT_AUTH_ALREADY_REJECTED(409806, "该申请已审核，不能重复驳回", HttpStatus.CONFLICT),
    STUDENT_NO_USED_BY_OTHER(409807, "该学号已被其他账号认证", HttpStatus.CONFLICT),
    FOLLOW_SELF_NOT_ALLOWED(400801, "不能关注自己", HttpStatus.BAD_REQUEST),

    SEARCH_FAILURE_NOT_FOUND(404901, "失败记录不存在", HttpStatus.NOT_FOUND),
    SEARCH_FAILURE_CHANNEL_UNSUPPORTED(400901, "不支持的失败记录通道", HttpStatus.BAD_REQUEST),
    SEARCH_REPLAY_FAILED(500901, "重放失败", HttpStatus.INTERNAL_SERVER_ERROR);

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
