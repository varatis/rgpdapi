package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.DataException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional // Default for all read operations
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;
    private final ClientMapper clientMapper;

    @Override
    public Page<TraitementPartielDTO> getTraitements(Pageable pageable) {
        Page<Traitement> traitementList = traitementRepository.findAll(pageable);
        return traitementMapper.toTraitementPartielDTOPage(traitementList);
    }

    @Override
    public TraitementDTO getOneTraitement(int id) {
        Traitement traitement = traitementRepository.findByIdFonctionnel(id);
        return traitementMapper.mapToDTO(traitement);
    }

    @Override
    public TraitementDTO createTraitement(TraitementDTO traitementDTO){
        Optional<Traitement> byIdFonctionnelAndNomAndClient = traitementRepository.findByIdFonctionnelAndNomAndClient(traitementDTO.idFonctionnel(), traitementDTO.nom(), clientMapper.map(traitementDTO.client()));
        if (byIdFonctionnelAndNomAndClient.isEmpty()) {
            return traitementMapper.mapToDTO(traitementRepository.save(traitementMapper.mapToTraitement(traitementDTO)));
        } else {
            throw new DataException("Traitement déjà présent en base pour cet Id Fonctionnel, nom et client", new SQLException("Data Already in DB"));
        }
    }

    @Override
    public Integer getNextIdFonctionnel() {
        return traitementRepository.findMaxIdFonctionnel().orElse(0)+1;
    }
}
