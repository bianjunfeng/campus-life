package com.campus.campus_life_backend.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具
 * 用于生成 BCrypt 密码哈希值，修复数据库中的明文密码
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 演示账号密码
        String demoPassword = "123456";
        String hashedPassword = encoder.encode(demoPassword);

        System.out.println("=".repeat(60));
        System.out.println("密码哈希生成工具");
        System.out.println("=".repeat(60));
        System.out.println("\n明文密码: " + demoPassword);
        System.out.println("BCrypt 哈希值: " + hashedPassword);

        // 验证密码
        boolean matches = encoder.matches(demoPassword, hashedPassword);
        System.out.println("验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));

        // 生成 SQL 更新语句
        System.out.println("\n" + "=".repeat(60));
        System.out.println("SQL 更新语句（用于修复演示账号 13800138000）:");
        System.out.println("=".repeat(60));
        System.out.println("UPDATE `user`");
        System.out.println("SET password_hash = '" + hashedPassword + "',");
        System.out.println("    update_time = NOW()");
        System.out.println("WHERE phone = '13800138000';");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("提示：每次运行都会生成不同的哈希值（这是正常的）");
        System.out.println("     但都能正确验证相同的明文密码");
        System.out.println("=".repeat(60));
    }
}


