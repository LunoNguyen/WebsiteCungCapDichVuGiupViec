package com.example.WebsiteCungCapDichVuGiupViec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example")
@EntityScan(basePackages = "com.example.Model")
@EnableJpaRepositories(basePackages = "com.example.Repository")
@EnableScheduling
public class WebsiteCungCapDichVuGiupViecApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebsiteCungCapDichVuGiupViecApplication.class, args);
	}

}
