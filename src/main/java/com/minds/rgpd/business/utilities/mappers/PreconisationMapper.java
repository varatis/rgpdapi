package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.minds.rgpd.persistence.entities.Preconisation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring", uses = ClientMapper.class)
public interface PreconisationMapper {

    @Mapping(target = "traitementIdentifiant", source = "traitement.identifiant")
    @Mapping(target = "traitementIdFonctionnel", source = "traitement.idFonctionnel")
    @Mapping(target = "traitementNom", source = "traitement.nom")
    PreconisationDTO mapToDTO(Preconisation preconisation);

    @Mapping(target = "traitementIdentifiant", source = "traitement.identifiant")
    @Mapping(target = "traitementNom", source = "traitement.nom")
    PreconisationPartielDTO map(Preconisation preconisation);

    /**
     * Le client et le traitement sont résolus par le service à partir de leurs
     * identifiants : ils sont donc ignorés ici pour ne pas rattacher des
     * instances transitoires issues du DTO.
     */
    @Mapping(target = "identifiant", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "traitement", ignore = true)
    Preconisation mapToPreconisation(PreconisationDTO preconisationDTO);

    /**
     * Reporte les champs métier du DTO sur la préconisation managée.
     * L'identifiant, le client et le traitement sont réaffectés par le service.
     */
    @Mapping(target = "identifiant", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "traitement", ignore = true)
    void updatePreconisationFromDto(PreconisationDTO preconisationDTO, @MappingTarget Preconisation preconisation);

    default Page<PreconisationPartielDTO> toPartielDTOPage(Page<Preconisation> page) {
        if (Objects.isNull(page)) {
            return null;
        }
        return new PageImpl<>(mapToPartialDTOList(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    List<PreconisationPartielDTO> mapToPartialDTOList(List<Preconisation> preconisations);
}