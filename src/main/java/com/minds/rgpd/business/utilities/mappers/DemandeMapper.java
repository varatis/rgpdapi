package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.persistence.entities.Demande;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DemandeMapper {

    @Mapping(source = "client.id", target = "clientId")
    DemandeDTO map(Demande demande);

    List<DemandeDTO> mapToDTOList(List<Demande> demandes);

    /*
     * Pour l'instant on ignore le client lors du mapping DTO -> Entity.
     * Il sera alimenté dans le Service à partir du ClientRepository.
     */
    @Mapping(target = "client", ignore = true)
    Demande map(DemandeDTO demandeDTO);

    List<Demande> mapToDemandeList(List<DemandeDTO> demandesDTO);
}