package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import java.util.List;
import java.util.UUID;

public interface UtilisateurService {

    List<UtilisateurDTO> rechercher(UtilisateurFilterCriteria criteres);

    UtilisateurDTO creer(UtilisateurWriteDTO payload);

    UtilisateurDTO modifier(UUID id, UtilisateurWriteDTO payload);

    void supprimer(UUID id);

    UtilisateurDTO getUtilisateurParId(UUID id);

    List<String> listerRolesDisponibles();
}