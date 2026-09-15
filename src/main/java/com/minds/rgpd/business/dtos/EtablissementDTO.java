package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record EtablissementDTO(
        UUID id,

        @NotBlank
        @Size(max = 255)
        String nom,

        // Trois caracteres : les codes DOM-TOM (971) et ceux de la Corse (2A, 2B).
        @Size(max = 3)
        String departement,

        boolean principal,

        @NotNull
        ClientDTO client
) {
}
