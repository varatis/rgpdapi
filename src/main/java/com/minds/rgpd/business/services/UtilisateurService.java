package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UtilisateurService {

    List<UtilisateurDTO> getUtilisateurs();
}
