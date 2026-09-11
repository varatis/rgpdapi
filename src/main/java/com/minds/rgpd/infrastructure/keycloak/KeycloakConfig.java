package com.minds.rgpd.infrastructure.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Assemblage de la brique Keycloak : le client d'administration est construit
 * manuellement (il n'est ni un {@code @Component} ni configurable par annotations).
 */
@Configuration
public class KeycloakConfig {

    @Bean
    public RestTemplate keycloakRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public KeycloakAdminClient keycloakAdminClient(KeycloakProperties properties, RestTemplate keycloakRestTemplate) {
        return new KeycloakAdminClient(properties, keycloakRestTemplate);
    }
}
