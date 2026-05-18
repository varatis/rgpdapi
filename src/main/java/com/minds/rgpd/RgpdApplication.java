package com.minds.rgpd;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "${spring.application.name}",
        version = "${application.version}",
        description = "${spring.application.description}"
))
public class RgpdApplication {

    public static void main(String[] args) {
        SpringApplication.run(RgpdApplication.class, args);
    }
}
