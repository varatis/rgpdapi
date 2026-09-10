package com.minds.rgpd.infrastructure.keycloak;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {

    private boolean enabled = false;
    private String baseUrl = "http://localhost:8081";
    private String realm = "minds-rgpd";
    private String adminClientId = "minds-saas-rgpd-admin";
    private String adminClientSecret;
    private String resourceClientId = "minds-saas-rgpd";
    private int taillePage = 100;
    private int nombreMaxResultats = 2000;
    private Duration delaiConnexion = Duration.ofSeconds(5);
    private Duration delaiLecture = Duration.ofSeconds(15);
    private boolean desactiverVerificationSsl = false;

    public String urlAdmin() {
        return racine() + "/admin/realms/" + realm;
    }

    public String urlJeton() {
        return racine() + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private String racine() {
        String racine = baseUrl == null ? "" : baseUrl.trim();
        return racine.endsWith("/") ? racine.substring(0, racine.length() - 1) : racine;
    }
}
