package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.persistence.entities.Traitement;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TraitementMapper {

    TraitementDTO mapToDTO(Traitement traitement);

    List<TraitementDTO> mapToDTOList(List<Traitement> traitements);

    Traitement mapToTraitement(TraitementDTO traitementDTO);

    List<Traitement> mapToTraitementList(List<TraitementDTO> traitementsDTO);

}
