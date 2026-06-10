package com.minds.rgpd.business.dtos;

import java.util.UUID;

public record ProfilDTO(
        UUID id,
        String code,
        String description
) {
}
