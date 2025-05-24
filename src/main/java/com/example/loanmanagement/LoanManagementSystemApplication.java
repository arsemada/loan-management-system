package com.example.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LoanManagementSystemApplication {

	public static void main(String[] args) {

		SpringApplication.run(LoanManagementSystemApplication.class, args);

	}

}
