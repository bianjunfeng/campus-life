package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;

public class FailingSmsSender implements SmsSender {

    private final String message;

    public FailingSmsSender() {
        this("短信服务未配置，无法发送验证码");
    }

    public FailingSmsSender(String message) {
        this.message = message;
    }

    @Override
    public void sendVerificationCode(String phone, String code, VerificationScene scene) {
        throw new BusinessException(
                BusinessErrorCode.SERVICE_UNAVAILABLE,
                message
        );
    }
}
