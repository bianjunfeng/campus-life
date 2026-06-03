package com.campus.campus_life_ai.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.campus.campus_life_ai.ai.mapper",
        "com.campus.campus_life_ai.knowledge.mapper"
})
public class MyBatisConfig {
}
