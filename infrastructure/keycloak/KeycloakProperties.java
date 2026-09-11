package com.minds.rgpd.infrastructure.keycloak;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration Keycloak (préfixe {@code keycloak}).
 *
 * <p>Liaison en mode JavaBean : Spring Boot instancie la classe via son
 * constructeur par défaut puis injecte chaque propriété par son setter —
 * ces derniers sont donc obligatoires.</p>
 */
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private boolean enabled = true;

    private String baseUrl;

    private String realm = "minds-rgpd";

    private String adminClientId = "minds-rgpd-admin";

    private String adminClientSecret;

    private String resourceClientId = "minds-saas-rgpd";

    private String groupPrefix = "/clients";

    private int pageSize = 200;

    private Duration tokenExpirationMarge = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public void setAdminClientId(String adminClientId) {
        this.adminClientId = adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public void setAdminClientSecret(String adminClientSecret) {
        this.adminClientSecret = adminClientSecret;
    }

    public String getResourceClientId() {
        return resourceClientId;
    }

    public void setResourceClientId(String resourceClientId) {
        this.resourceClientId = resourceClientId;
    }

    public String getGroupPrefix() {
        return groupPrefix;
    }

    public void setGroupPrefix(String groupPrefix) {
        this.groupPrefix = groupPrefix;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Duration getTokenExpirationMarge() {
        return tokenExpirationMarge;
    }

    public void setTokenExpirationMarge(Duration tokenExpirationMarge) {
        this.tokenExpirationMarge = tokenExpirationMarge;
    }
}
