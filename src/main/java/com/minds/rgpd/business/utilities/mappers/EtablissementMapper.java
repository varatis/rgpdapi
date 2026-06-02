package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.persistence.entities.Etablissement;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EtablissementMapper {

    Etablissement map(EtablissementDTO etablissementDTO);

    List<Etablissement> mapToList(List<EtablissementDTO> etablissementDTO);

    EtablissementDTO map(Etablissement etablissement);

    List<EtablissementDTO> mapToDTOList(List<Etablissement> etablissement);
}
