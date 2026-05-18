package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.services.EtablissementService;
import com.minds.rgpd.business.utilities.mappers.EtablissementMapper;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default for all read operations
public class EtablissementServiceImpl implements EtablissementService {

    private final EtablissementRepository etablissementRepository;
    private final EtablissementMapper etablissementMapper;

    @Override
    public List<EtablissementDTO> getEtablissements() {
        List<Etablissement> etablissements = etablissementRepository.findAll();
        return etablissementMapper.mapToDTOList(etablissements);
    }
}
