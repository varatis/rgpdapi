package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UtilisateurWriteDTO(
        @NotBlank(message = "est obligatoire")
        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String prenom,
        @NotBlank(message = "est obligatoire")
        @Size(max = 100, message = "ne doit pas dépasser 100 caractères")
        String nom,
        @NotBlank(message = "est obligatoire")
        @Email(message = "doit être une adresse email valide")
        @Size(max = 255, message = "ne doit pas dépasser 255 caractères")
        String email,
        @NotEmpty(message = "doit contenir au moins un rôle")
        List<String> roles,
        UUID clientId,
        String groupe,
        Boolean actif
) {
}
