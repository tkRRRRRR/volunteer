package com.example.volunteer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI volunteerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("高考志愿填报辅助系统 API")
                .description("本科毕业设计首版接口")
                .version("0.1.0"));
    }
}
