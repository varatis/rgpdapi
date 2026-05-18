package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.persistence.entities.Utilisateur;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UtilisateurMapper {

    Utilisateur map(UtilisateurDTO utilisateurDTO);
    List<Utilisateur> mapToList(List<UtilisateurDTO> utilisateurDTO);

    UtilisateurDTO map(Utilisateur utilisateur);
    List<UtilisateurDTO> mapToDTOList(List<Utilisateur> utilisateur);
}
