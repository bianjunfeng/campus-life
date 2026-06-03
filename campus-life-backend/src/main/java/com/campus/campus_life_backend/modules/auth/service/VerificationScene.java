package com.campus.campus_life_backend.modules.auth.service;

import java.util.Arrays;

public enum VerificationScene {
    REGISTER("register"),
    LOGIN("login"),
    RESET_PASSWORD("reset_password"),
    BIND_PHONE("bind_phone");

    private final String code;

    VerificationScene(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static VerificationScene fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("验证码场景不能为空");
        }
        String normalized = code.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(scene -> scene.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的验证码场景: " + code));
    }
}
