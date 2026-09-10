package com.minds.rgpd.business.identity;

import java.util.List;

public record IdentiteCommande(
        String prenom,
        String nom,
        String email,
        List<String> roles,
        String groupe,
        boolean actif
) {

    public IdentiteCommande {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
