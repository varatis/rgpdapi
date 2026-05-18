package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ProfilDTO;
import com.minds.rgpd.business.services.ProfilService;
import com.minds.rgpd.business.utilities.mappers.ProfilMapper;
import com.minds.rgpd.persistence.entities.Profil;
import com.minds.rgpd.persistence.repositories.ProfilRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfilServiceImpl implements ProfilService {

    private final ProfilRepository profilRepository;
    private final ProfilMapper profilMapper;


    @Override
    public List<ProfilDTO> getProfils() {
        List<Profil> profils = profilRepository.findAll();
        return profilMapper.mapToDTOList(profils);
    }
}
