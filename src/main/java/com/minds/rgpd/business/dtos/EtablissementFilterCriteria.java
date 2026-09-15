package com.minds.rgpd.business.dtos;

public record EtablissementFilterCriteria(
        String nom,
        String departement,
        Boolean principal
) {
    public static EtablissementFilterCriteria empty() {
        return new EtablissementFilterCriteria(null, null, null);
    }
}
