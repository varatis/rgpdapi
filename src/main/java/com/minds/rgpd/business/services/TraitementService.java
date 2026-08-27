package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TraitementService {

    Page<TraitementPartielDTO> getTraitements(Pageable pageable, String clientName, TraitementFilterCriteria criteria);

    TraitementDTO getOneTraitement(int id);

    TraitementDTO createTraitement(TraitementDTO traitement);

    TraitementDTO updateTraitement(int idFonctionnel, TraitementDTO traitement);

    /**
     * RG1 : historique des modifications du traitement, de la plus récente à la
     * plus ancienne (date + motif saisi par l'utilisateur, CA4).
     */
    List<HistorisationDTO> getHistoriqueTraitement(int idFonctionnel);

    Integer getNextIdFonctionnel();

    Integer deleteDuplicateTraitements();

    void deleteTraitementById(UUID id);
}
