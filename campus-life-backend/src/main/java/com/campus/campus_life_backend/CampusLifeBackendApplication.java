package com.campus.campus_life_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusLifeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusLifeBackendApplication.class, args);
	}

}
