package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PreconisationService {

    Page<PreconisationPartielDTO> getPreconisations(Pageable pageable, String clientNom, PreconisationFilterCriteria criteria);

    PreconisationDTO getOnePreconisation(UUID id);

    PreconisationDTO createPreconisation(PreconisationDTO preconisationDTO);

    PreconisationDTO updatePreconisation(UUID id, PreconisationDTO preconisationDTO);

    /**
     * RG1 : historique des modifications de la préconisation, de la plus
     * récente à la plus ancienne (date + motif saisi par l'utilisateur, CA4).
     */
    List<HistorisationDTO> getHistoriquePreconisation(UUID id);

    void deletePreconisationById(UUID id);
}