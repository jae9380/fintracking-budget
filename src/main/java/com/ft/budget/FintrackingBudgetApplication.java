package com.ft.budget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class FintrackingBudgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintrackingBudgetApplication.class, args);
    }

}
