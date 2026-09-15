package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.persistence.entities.Etablissement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", uses = ClientRefMapper.class)
public interface EtablissementMapper {

    Etablissement map(EtablissementDTO etablissementDTO);

    List<Etablissement> mapToList(List<EtablissementDTO> etablissementDTO);

    EtablissementDTO map(Etablissement etablissement);

    List<EtablissementDTO> mapToDTOList(List<Etablissement> etablissement);

    default Page<EtablissementDTO> mapToDTOPage(Page<Etablissement> page) {
        if (Objects.isNull(page)) {
            return null;
        }
        return new PageImpl<>(mapToDTOList(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    /**
     * L'identifiant et le client sont ignores : le premier est porte par l'URL,
     * le second est rattache par le service depuis la base, l'instance issue du
     * DTO etant detachee.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    void updateEtablissementFromDto(EtablissementDTO etablissementDTO, @MappingTarget Etablissement etablissement);
}
