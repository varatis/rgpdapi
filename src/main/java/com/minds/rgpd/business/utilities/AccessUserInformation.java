package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

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

            // Les rôles sont lus depuis les autorités déjà résolues par JwtAuthConverter
            // (resource_access.<client>.roles), plutôt que de re-parser la claim ici.
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return new CurrentUser(identifiant, nom, email, roles);
        }
        return null;
    }

}
