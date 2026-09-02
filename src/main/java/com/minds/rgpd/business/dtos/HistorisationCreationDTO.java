package com.minds.rgpd.business.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record HistorisationCreationDTO(
        @NotBlank
        @Size(max = 2000)
        String motif,
        LocalDateTime date
) {
}
