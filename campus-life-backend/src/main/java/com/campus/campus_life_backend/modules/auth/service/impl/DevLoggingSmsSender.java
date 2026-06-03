package com.campus.campus_life_backend.modules.auth.service.impl;

import com.campus.campus_life_backend.modules.auth.service.SmsSender;
import com.campus.campus_life_backend.modules.auth.service.VerificationScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevLoggingSmsSender implements SmsSender {

    private static final Logger logger = LoggerFactory.getLogger(DevLoggingSmsSender.class);

    @Override
    public void sendVerificationCode(String phone, String code, VerificationScene scene) {
        logger.info("开发环境短信验证码 scene={}, phone={}, code={}", scene.code(), phone, code);
    }
}
