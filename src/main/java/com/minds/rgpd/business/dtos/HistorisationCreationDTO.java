package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Saisie par l'utilisateur d'une entrée d'historique (CA4).
 * <p>
 * La date est facultative : à défaut, l'horodatage courant est utilisé.
 * L'auteur n'est jamais accepté depuis le client : il est déduit du jeton.
 */
@Builder
public record HistorisationCreationDTO(
        @NotBlank
        @Size(max = 2000)
        String motif,
        LocalDateTime date
) {
}
