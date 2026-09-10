package com.minds.rgpd.business.identity;

import java.util.List;
import java.util.UUID;

public record IdentiteUtilisateur(
        UUID id,
        String identifiant,
        String prenom,
        String nom,
        String email,
        boolean actif,
        List<String> roles,
        String groupe
) {
}