package com.minds.rgpd.business.dtos;

/** Contenu binaire du logo et ce qu'il faut pour construire la réponse HTTP. */
public record ClientLogoContentDTO(
        byte[] content,
        String contentType,
        String etag
) {
}
