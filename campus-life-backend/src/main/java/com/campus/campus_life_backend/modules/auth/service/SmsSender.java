package com.campus.campus_life_backend.modules.auth.service;

public interface SmsSender {
    void sendVerificationCode(String phone, String code, VerificationScene scene);
}
