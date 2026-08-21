package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.PreconisationService;
import com.minds.rgpd.business.utilities.mappers.PreconisationMapper;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.specifications.PreconisationSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PreconisationServiceImpl implements PreconisationService {

    private final PreconisationRepository preconisationRepository;
    private final PreconisationMapper preconisationMapper;

    @Override
    public Page<PreconisationPartielDTO> getPreconisations(
            Pageable pageable,
            String clientNom,
            PreconisationFilterCriteria criteria
    ) {
        if (clientNom == null || clientNom.isBlank()) {
            return Page.empty(pageable);
        }

        PreconisationFilterCriteria safe = criteria != null ? criteria : PreconisationFilterCriteria.empty();
        Specification<Preconisation> spec = PreconisationSpecifications.search(
                clientNom,
                safe.libelle(),
                safe.etatAvancement(),
                safe.idTraitement()
        );

        Page<Preconisation> page = preconisationRepository.findAll(spec, pageable);
        return preconisationMapper.toPartielDTOPage(page);
    }

    @Override
    public PreconisationDTO getOnePreconisation(UUID id) {
        Preconisation preconisation = preconisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préconisation", "id", id));
        return preconisationMapper.mapToDTO(preconisation);
    }
}
