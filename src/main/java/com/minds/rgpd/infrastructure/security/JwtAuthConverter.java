package com.minds.rgpd.infrastructure.security;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthConverter.class);

    // Préfixe attendu par hasRole(...) : hasRole('ADMIN') teste l'autorité "ROLE_ADMIN".
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    // Identifiant du client Keycloak dont on lit les rôles dans resource_access.
    private final String resourceId;

    public JwtAuthConverter(@Value("${application.security.jwt.resource-id:minds-saas-rgpd}") String resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        // convert() peut retourner null lorsque le token ne porte aucun scope.
        Collection<GrantedAuthority> scopes = jwtGrantedAuthoritiesConverter.convert(jwt);
        Stream<GrantedAuthority> scopeStream = scopes == null ? Stream.empty() : scopes.stream();

        Set<GrantedAuthority> authorities = Stream
                .concat(scopeStream, extractResourceRoles(jwt).stream())
                .collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaim("preferred_username"));
    }

    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        logger.debug("issuer = {}", jwt.getIssuer());

        // Les deux sources sont cumulées : un utilisateur peut porter à la fois
        // des groupes client et des rôles applicatifs.
        Set<GrantedAuthority> authorities = Stream
                .concat(clientGroups(jwt).stream(), resourceRoles(jwt).stream())
                .map(this::toAuthority)
                .collect(Collectors.toSet());

        logger.debug("authorities = {}", authorities);
        return authorities;
    }

    /**
     * Rôles applicatifs portés par la claim resource_access :
     * <pre>
     * "resource_access": {
     *   "minds-saas-rgpd": { "roles": ["admin"] }
     * }
     * </pre>
     */
    private Collection<String> resourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) {
            return List.of();
        }
        if (!(resourceAccess.get(resourceId) instanceof Map<?, ?> resource)) {
            logger.debug("Aucun bloc resource_access pour le client {}", resourceId);
            return List.of();
        }
        if (!(resource.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return asStringList(roles);
    }

    private Collection<String> clientGroups(Jwt jwt) {
        if (!(jwt.getClaim("client_groups") instanceof Collection<?> groups)) {
            return List.of();
        }
        return asStringList(groups);
    }

    private Collection<String> asStringList(Collection<?> values) {
        return values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    /**
     * Normalise en "ROLE_<MAJUSCULES>" : Keycloak renvoie les rôles en minuscules
     * ("admin") et les groupes sous forme de chemin ("/admin"), alors que
     * hasRole('ADMIN') compare littéralement à "ROLE_ADMIN".
     */
    private GrantedAuthority toAuthority(String role) {
        String normalized = role.startsWith("/") ? role.substring(1) : role;
        return new SimpleGrantedAuthority(ROLE_PREFIX + normalized.toUpperCase(Locale.ROOT));
    }
}
