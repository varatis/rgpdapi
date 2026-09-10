package com.minds.rgpd.business.dtos;

import java.util.List;
import java.util.UUID;

public record UtilisateurDTO(
        UUID id,
        String identifiant,
        String prenom,
        String nom,
        String email,
        boolean actif,
        List<String> roles,
        UUID clientId,
        String clientNom
) {
}