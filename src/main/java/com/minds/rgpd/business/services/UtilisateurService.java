package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface UtilisateurService {

    Page<UtilisateurDTO> getUtilisateurs(Pageable pageable, UtilisateurFilterCriteria criteria);

    UtilisateurDTO getUtilisateur(UUID id);

    UtilisateurDTO creerUtilisateur(UtilisateurWriteDTO payload);

    UtilisateurDTO modifierUtilisateur(UUID id, UtilisateurWriteDTO payload);

    void supprimerUtilisateur(UUID id);

    List<String> getRoles();
}
