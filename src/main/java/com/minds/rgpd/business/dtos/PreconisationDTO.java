package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record PreconisationDTO(
        @NotNull
        UUID identifiant,
        @NotNull
        String libelle,
        String explication,
        String risqueEncours,
        String contraintes,
        String cout,
        String priorite,
        String complexite,
        String commentaire,
        String etatAvancement,
        ClientDTO client,
        UUID traitementIdentifiant,
        Integer traitementIdFonctionnel,
        String traitementNom
) {
}
