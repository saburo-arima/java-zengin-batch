package com.example.zengin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.example.zengin")
@EnableJpaRepositories("com.example.zengin")
public class ZenginBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZenginBatchApplication.class, args);
	}

}
