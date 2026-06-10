package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.persistence.entities.Traitement;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface TraitementMapper {

    TraitementDTO mapToDTO(Traitement traitement);

    default Page<TraitementPartielDTO> toTraitementPartielDTOPage(Page<Traitement> traitementsPage) {
        if (Objects.isNull(traitementsPage)) {
            return null;
        }
        List<TraitementPartielDTO> partielDTOs = mapToPartialDTOList(traitementsPage.getContent());
        return new PageImpl<>(partielDTOs, traitementsPage.getPageable(), traitementsPage.getTotalElements());
    }

    List<TraitementPartielDTO> mapToPartialDTOList(List<Traitement> traitements);

    TraitementPartielDTO map(Traitement traitement);

    List<TraitementDTO> mapToDTOList(List<Traitement> traitements);

    Traitement mapToTraitement(TraitementDTO traitementDTO);

    List<Traitement> mapToTraitementList(List<TraitementDTO> traitementsDTO);

}
