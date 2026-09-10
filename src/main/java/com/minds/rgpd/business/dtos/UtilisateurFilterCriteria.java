package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record UtilisateurFilterCriteria(
        String nom,
        String prenom,
        UUID clientId
) {

    public static UtilisateurFilterCriteria empty() {
        return new UtilisateurFilterCriteria(null, null, null);
    }
}
