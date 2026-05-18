package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default for all read operations
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;

    @Override
    public List<TraitementDTO> getTraitements() {
        List<Traitement> traitementList = traitementRepository.findAll();

        return traitementMapper.mapToDTOList(traitementList);
    }
}
