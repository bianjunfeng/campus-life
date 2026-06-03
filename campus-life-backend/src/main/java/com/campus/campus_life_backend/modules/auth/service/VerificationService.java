package com.campus.campus_life_backend.modules.auth.service;

public interface VerificationService {
    /**
     * 发送验证码到手机
     */
    default void sendVerificationCode(String phone) {
        sendVerificationCode(phone, VerificationScene.LOGIN.code(), null);
    }

    default void sendVerificationCode(String phone, String scene) {
        sendVerificationCode(phone, scene, null);
    }

    void sendVerificationCode(String phone, String scene, String clientIp);
    
    /**
     * 验证验证码
     */
    default boolean verifyCode(String phone, String code) {
        return verifyCode(phone, code, VerificationScene.LOGIN.code());
    }

    boolean verifyCode(String phone, String code, String scene);
}


