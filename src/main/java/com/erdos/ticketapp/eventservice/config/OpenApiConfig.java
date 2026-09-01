package com.erdos.ticketapp.eventservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Service API")
                        .description("API documentation for the TicketApp event service")
                        .version("v1"));
    }
}
