package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.services.UtilisateurService;
import com.minds.rgpd.business.utilities.mappers.UtilisateurMapper;
import com.minds.rgpd.persistence.entities.Utilisateur;
import com.minds.rgpd.persistence.repositories.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurMapper utilisateurMapper;

    @Override
    public List<UtilisateurDTO> getUtilisateurs() {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        return utilisateurMapper.mapToDTOList(utilisateurs);
    }
}
