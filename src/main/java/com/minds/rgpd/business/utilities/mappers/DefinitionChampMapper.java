package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.DefinitionChampDTO;
import com.minds.rgpd.persistence.entities.DefinitionChamp;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DefinitionChampMapper {

    DefinitionChampDTO map(DefinitionChamp definitionChamp);

    List<DefinitionChampDTO> mapToDTOList(List<DefinitionChamp> definitionChamps);
}
