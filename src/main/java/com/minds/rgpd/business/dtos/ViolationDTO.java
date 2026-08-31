package com.minds.rgpd.business.dtos;

import com.minds.rgpd.business.enums.ViolationStatut;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ViolationDTO(

        UUID identifiant,

        @NotNull
        ClientDTO client,

        LocalDate dateViolation,

        String natureViolation,

        String donneesConcernees,

        Integer nombreApproximatifDonneesConcernees,

        String categoriesPersonnesConcernees,

        Integer nombrePersonnesConcernees,

        String consequences,

        String mesuresPrisesPrevues,

        String informationCnil,

        Boolean risqueEleveDroitsLibertes,

        String communicationPersonnesEffectueeEtDate,

        String commentaires,

        ViolationStatut statut
) {
}
