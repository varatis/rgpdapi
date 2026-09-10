package com.minds.rgpd.infrastructure.keycloak;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public String getAdminClientId() {
        return adminClientId;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public String getResourceClientId() {
        return resourceClientId;
    }

    public String getGroupPrefix() {
        return groupPrefix;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Duration getTokenExpirationMarge() {
        return tokenExpirationMarge;
    }
}