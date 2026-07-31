package com.minds.rgpd.business.dtos;

public record TraitementFilterCriteria(
        String nom,
        String gestionnaireMiseEnOeuvre,
        String finalitePrincipale
) {
    public static TraitementFilterCriteria empty() {
        return new TraitementFilterCriteria(null, null, null);
    }
}
