package com.example.sis1.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sis1OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIS 1 — Task API")
                        .description("Course project: CRUD + DTOs + Bean Validation + unified error format")
                        .version("1.0.0"));
    }
}
