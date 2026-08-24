package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record PreconisationFilterCriteria(
        String libelle,
        String etatAvancement,
        UUID idTraitement
) {
    public static PreconisationFilterCriteria empty() {
        return new PreconisationFilterCriteria(null, null, null);
    }
}
