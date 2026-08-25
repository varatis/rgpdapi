package com.minds.rgpd.business.dtos;

import com.minds.rgpd.business.enums.DemandeStatut;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record DemandeDTO(

        UUID id,

        String typeDemande,

        String descriptionSynthetique,

        String origine,

        LocalDate dateReception,

        String servicesConcernes,

        String detailTraitement,

        String servicesImpliques,

        String reponse,

        String alerteRt,

        DemandeStatut statut,

        UUID clientId

) {
}