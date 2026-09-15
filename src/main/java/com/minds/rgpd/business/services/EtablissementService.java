package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EtablissementService {

    Page<EtablissementDTO> getEtablissements(Pageable pageable, String clientNom, EtablissementFilterCriteria criteria);

    EtablissementDTO getOneEtablissement(UUID id);

    EtablissementDTO createEtablissement(EtablissementDTO etablissement);

    EtablissementDTO updateEtablissement(UUID id, EtablissementDTO etablissement);

    void deleteEtablissementById(UUID id);
}
