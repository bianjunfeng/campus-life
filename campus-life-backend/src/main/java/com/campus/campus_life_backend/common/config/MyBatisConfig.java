package com.campus.campus_life_backend.common.config;

/**
 * ClassName: MyBatisConfig
 * Description:
 * MyBatis 配置类
 *
 *
 * @Author Junfeng Bian
 * @Create 2025/11/29 20:24
 * @Version 1.0
 */

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 扫描配置
 * 使用 @MapperScan 后，Mapper 接口上不需要再写 @Mapper 注解
 * 可以简化为扫描父包，自动识别所有 *.mapper 包下的接口
 */
@Configuration
@MapperScan(basePackages = {
        "com.campus.campus_life_backend.common.event.mapper",
        "com.campus.campus_life_backend.modules.admin.mapper",
        "com.campus.campus_life_backend.modules.auth.mapper",
        "com.campus.campus_life_backend.modules.forum.mapper",
        "com.campus.campus_life_backend.modules.merchant.mapper",
        "com.campus.campus_life_backend.modules.message.mapper",
        "com.campus.campus_life_backend.modules.order.mapper",
        "com.campus.campus_life_backend.modules.payment.mapper",
        "com.campus.campus_life_backend.modules.user.mapper",
        "com.campus.campus_life_backend.modules.voucher.mapper"
})
public class MyBatisConfig {
}
