package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public final class AccessUserInformation {

    private AccessUserInformation() {
    }

    //Cette methode permet de récupérer les informations l'utilisateur connecté à partir d'un token JWT
    public static CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String identifiant = jwt.getClaimAsString("preferred_username");
            String nom = jwt.getClaimAsString("name");
            String email = jwt.getClaimAsString("email");

            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            List<String> roles = (List<String>) realmAccess.get("roles");
            return new CurrentUser(identifiant, nom, email, roles);
        }
        return null;
    }

}
