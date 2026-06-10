package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record TraitementPartielDTO(
        UUID identifiant,
        Integer idFonctionnel,
        String nom,
        String gestionnaire,
        String finalitePrincipale
) {
}
