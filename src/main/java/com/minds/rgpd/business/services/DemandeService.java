package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.DemandeDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface DemandeService {

    List<DemandeDTO> getDemandes();

    DemandeDTO getDemande(UUID id);

    DemandeDTO createDemande(DemandeDTO demandeDTO);

    DemandeDTO updateDemande(UUID id, DemandeDTO demandeDTO);

    void deleteDemandeById(UUID id);

    @Transactional
    DemandeDTO traiterDemande(UUID id);
}