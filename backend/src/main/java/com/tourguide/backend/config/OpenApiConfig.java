package com.tourguide.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI document metadata. The spec drives generated TS types for the frontends (MIN-14). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tourguideOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Tour-guide Platform API")
                .description("景区定制讲解服务预约管理 — backend API")
                .version("v0.1.0")
                .license(new License().name("Proprietary")));
    }
}
