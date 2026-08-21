package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PreconisationService {

    Page<PreconisationPartielDTO> getPreconisations(Pageable pageable, String clientNom, PreconisationFilterCriteria criteria);

    PreconisationDTO getOnePreconisation(UUID id);
}
