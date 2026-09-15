package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    /**
     * Clients portés par la revendication {@code client_groups}.
     * <p>
     * Keycloak la transmet tantôt en tableau JSON, tantôt en chaîne : la valeur
     * lue en chaîne arrive alors sous la forme {@code [Client A, Client B]}.
     */
    private static List<String> getClientsDuJeton(Jwt jwt) {
        if (Objects.isNull(jwt)) {
            return List.of();
        }
        if (jwt.getClaim("client_groups") instanceof Collection<?> groupes) {
            return groupes.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(groupe -> !groupe.isBlank())
                    .toList();
        }

        String claim = jwt.getClaimAsString("client_groups");
        if (Objects.isNull(claim) || claim.isBlank()) {
            return List.of();
        }
        String valeur = claim.trim();
        if (valeur.startsWith("[") && valeur.endsWith("]")) {
            valeur = valeur.substring(1, valeur.length() - 1);
        }
        return Arrays.stream(valeur.split(","))
                .map(String::trim)
                .filter(groupe -> !groupe.isBlank())
                .toList();
    }

    /**
     * Client unique de l'utilisateur connecté, pour les écrans réservés aux
     * comptes rattachés à un seul client. Un jeton qui n'en porte aucun, ou qui
     * en porte plusieurs, ne permet pas de désigner le client à afficher.
     */
    public static String getClientUniqueDuJeton(Jwt jwt) {
        List<String> clients = getClientsDuJeton(jwt);
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("Aucun client associé à l'utilisateur connecté");
        }
        if (clients.size() > 1) {
            throw new IllegalArgumentException(
                    "Plusieurs clients associés à l'utilisateur connecté : " + String.join(", ", clients));
        }
        return clients.getFirst();
    }

}
