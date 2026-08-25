package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record PreconisationPartielDTO(
        UUID identifiant,
        String libelle,
        String priorite,
        String complexite,
        String etatAvancement,
        UUID traitementIdentifiant,
        String traitementNom
) {
}