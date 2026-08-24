package com.minds.rgpd.business.dtos;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record ClientDTO(
        UUID id,
        String nom,
        String statut,
        String version,
        LocalDate dateVersion,
        List<DureeDTO> durees,
        List<DefinitionDTO> definitions,
        List<ResponsableTraitementDTO> responsablesTraitement
) {
}
