package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default for all read operations
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final TraitementMapper traitementMapper;

    @Override
    public Page<TraitementPartielDTO> getTraitements(Pageable pageable) {
        Page<Traitement> traitementList = traitementRepository.findAll(pageable);
        return traitementMapper.toTraitementPartielDTOPage(traitementList);
    }

    @Override
    public TraitementDTO getOneTraitement(Integer id) {
        Traitement traitement = traitementRepository.findById(id).orElse(null);
        return traitementMapper.mapToDTO(traitement);
    }
}
