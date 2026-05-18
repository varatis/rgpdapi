package com.minds.rgpd.business.dtos;

public record UtilisateurDTO(
        Integer id,
        String prenom,
        String nom,
        String email,
        String password,
        String fonction,
        ProfilDTO profil,
        ClientDTO client
) {
}
