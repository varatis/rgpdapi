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

        /**
         * RG1 / CA4 : motif de la modification, renseigné par l'utilisateur à la
         * mise à jour ; alimente la table historisation_preconisation. Non
         * exploité à la création ni restitué en lecture.
         */
        String motifModification,

        ClientDTO client,
        UUID traitementIdentifiant,
        Integer traitementIdFonctionnel,
        String traitementNom
) {
}
