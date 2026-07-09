package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TraitementService {

    Page<TraitementPartielDTO> getTraitements(Pageable pageable, String clientName);

    TraitementDTO getOneTraitement(int id);

    TraitementDTO createTraitement(TraitementDTO traitement);

    TraitementDTO updateTraitement(int idFonctionnel, TraitementDTO traitement);

    Integer getNextIdFonctionnel();

    Integer deleteDuplicateTraitements();
}
