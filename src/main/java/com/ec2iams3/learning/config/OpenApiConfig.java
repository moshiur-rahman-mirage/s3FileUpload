package com.ec2iams3.learning.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EC3 File Upload API")
                        .version("1.0.0")
                        .description("REST API for uploading files to local disk or S3 storage.")
                        .contact(new Contact()
                                .name("EC3 team")
                                .email("support@example.com")));
    }
}
