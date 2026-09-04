package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload de création et de modification d'un client.
 * <p>
 * Ne porte que les colonnes scalaires de {@code CLIENT}. Les référentiels
 * rattachés — durées, définitions, responsables de traitement — sont alimentés
 * par le domaine (voir {@code DefinitionResolver}, {@code DureeResolver}) et non
 * par cet écran : les accepter ici les rendrait modifiables hors de leur cycle
 * de vie.
 * <p>
 * Les tailles reprennent celles des colonnes : {@code nom VARCHAR(255) NOT NULL},
 * {@code statut VARCHAR(100)}, {@code version VARCHAR(20)}.
 */
public record ClientWriteDTO(
        @NotBlank(message = "est obligatoire")
        @Size(max = 255, message = "ne doit pas dépasser 255 caractères")
        String nom,

        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String statut,

        @Size(max = 20, message = "ne doit pas dépasser 20 caractères")
        String version,

        LocalDate dateVersion
) {
}
