package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record UtilisateurDTO(
        UUID id,
        String prenom,
        String nom,
        String email,
        String password,
        String fonction,
        ProfilDTO profil,
        ClientDTO client
) {
}
