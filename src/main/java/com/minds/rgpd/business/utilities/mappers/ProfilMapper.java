package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ProfilDTO;
import com.minds.rgpd.persistence.entities.Profil;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfilMapper {

    Profil map(ProfilDTO profilDTO);
    List<Profil> mapToList(List<ProfilDTO> profilDTO);

    ProfilDTO map(Profil profil);
    List<ProfilDTO> mapToDTOList(List<Profil> profil);
}
