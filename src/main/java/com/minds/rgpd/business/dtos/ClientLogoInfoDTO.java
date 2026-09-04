package com.minds.rgpd.business.dtos;

import lombok.Builder;

/**
 * Métadonnées du logo, sans le contenu binaire : alimente l'écran de
 * modification (« Logo actuel : acme-logo.png — 340 Ko — Remplacer »).
 * <p>
 * {@code nomFichier} provient du navigateur et n'est pas fiable : le front doit
 * l'échapper comme du texte.
 */
@Builder
public record ClientLogoInfoDTO(
        String nomFichier,
        Integer taille,
        String contentType,
        String etag
) {
}
