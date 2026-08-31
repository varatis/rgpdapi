package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ViolationDTO;
import com.minds.rgpd.business.dtos.ViolationPartielDTO;
import com.minds.rgpd.persistence.entities.Violation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Objects;

/**
 * Le client imbriqué est mappé par {@link ClientRefMapper} : réduit à son
 * identité, sans son référentiel. Sans cela MapStruct chercherait à instancier
 * {@link com.minds.rgpd.persistence.entities.Definition}, qui est abstraite.
 */
@Mapper(componentModel = "spring", uses = ClientRefMapper.class)
public interface ViolationMapper {

    ViolationDTO mapToDTO(Violation violation);

    List<ViolationDTO> mapToDTOList(List<Violation> violations);

    Violation mapToViolation(ViolationDTO violationDTO);

    List<Violation> mapToViolationList(List<ViolationDTO> violationsDTO);

    ViolationPartielDTO mapToPartialDTO(Violation violation);

    List<ViolationPartielDTO> mapToPartialDTOList(List<Violation> violations);

    default Page<ViolationPartielDTO> toPartielDTOPage(Page<Violation> page) {
        if (Objects.isNull(page)) {
            return null;
        }
        return new PageImpl<>(mapToPartialDTOList(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    /**
     * Le client et l'identifiant sont ignorés : le premier est rattaché par le
     * service depuis la base (l'instance issue du DTO serait détachée), le
     * second est porté par l'URL et non par le corps de la requête.
     */
    @Mapping(target = "identifiant", ignore = true)
    @Mapping(target = "client", ignore = true)
    void updateViolationFromDto(ViolationDTO violationDTO, @MappingTarget Violation violation);
}
