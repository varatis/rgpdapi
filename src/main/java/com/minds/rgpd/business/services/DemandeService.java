package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.DemandeDTO;

import java.util.List;
import java.util.UUID;

public interface DemandeService {

    List<DemandeDTO> getDemandes();

    DemandeDTO getDemande(UUID id);

    DemandeDTO createDemande(DemandeDTO demandeDTO);

}