package com.minds.rgpd.business.identity;

import java.util.UUID;

public record GroupeIdentite(
        UUID id,
        String nom,
        String chemin
) {
}