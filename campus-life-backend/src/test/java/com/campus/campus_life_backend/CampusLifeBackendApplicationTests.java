package com.campus.campus_life_backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("依赖外部ES/Rabbit/Redis基础设施，默认环境跳过")
class CampusLifeBackendApplicationTests {

	static {
		System.setProperty("csp.sentinel.log.dir", "/tmp/sentinel-logs");
	}

	@Test
	void contextLoads() {
	}

}
