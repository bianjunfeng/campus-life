package com.campus.campus_life_gateway;

import com.campus.campus_life_gateway.config.GatewayAuthProperties;
import com.campus.campus_life_gateway.config.GatewayGrayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GatewayAuthProperties.class, GatewayGrayProperties.class})
public class CampusLifeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLifeGatewayApplication.class, args);
    }
}
