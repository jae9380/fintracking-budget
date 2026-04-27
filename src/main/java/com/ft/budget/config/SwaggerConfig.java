package com.ft.budget.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Budget Service API")
                        .description("예산 설정, 한도 초과 알림, Chain of Responsibility 패턴")
                        .version("v1.0.0"));
    }
}
