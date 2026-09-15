package com.minds.rgpd.business.utilities;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessUserInformationTest {

    private Jwt jeton(Object clientGroups) {
        Jwt.Builder builder = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .claim("preferred_username", "alice");
        if (clientGroups != null) {
            builder.claim("client_groups", clientGroups);
        }
        return builder.build();
    }

    @Test
    void litLeClientQuandLaRevendicationEstUnTableau() {
        assertEquals("La breteche",
                AccessUserInformation.getClientUniqueDuJeton(jeton(List.of("La breteche"))));
    }

    @Test
    void litLeClientQuandLaRevendicationEstUneChaine() {
        // Keycloak transmet parfois la revendication en chaine : "[La breteche]".
        assertEquals("La breteche",
                AccessUserInformation.getClientUniqueDuJeton(jeton("[La breteche]")));
    }

    @Test
    void ignoreLesEntreesVidesDeLaRevendication() {
        assertEquals("La breteche",
                AccessUserInformation.getClientUniqueDuJeton(jeton("[ La breteche , ]")));
    }

    @Test
    void refuseUnJetonSansClient() {
        assertThrows(IllegalArgumentException.class,
                () -> AccessUserInformation.getClientUniqueDuJeton(jeton(null)));
        assertThrows(IllegalArgumentException.class,
                () -> AccessUserInformation.getClientUniqueDuJeton(jeton("[]")));
    }

    @Test
    void refuseUnJetonPortantPlusieursClients() {
        // Les ecrans concernes ne savent pas lequel des deux registres afficher.
        assertThrows(IllegalArgumentException.class,
                () -> AccessUserInformation.getClientUniqueDuJeton(jeton(List.of("La breteche", "Entreprise Alpha"))));
    }

    @Test
    void refuseUnJetonAbsent() {
        assertThrows(IllegalArgumentException.class, () -> AccessUserInformation.getClientUniqueDuJeton(null));
    }
}
