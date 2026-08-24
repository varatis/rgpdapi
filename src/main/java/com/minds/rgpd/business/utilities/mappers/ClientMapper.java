package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.DefinitionDTO;
import com.minds.rgpd.business.dtos.DureeDTO;
import com.minds.rgpd.business.dtos.ResponsableTraitementDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Definition;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    /**
     * Les definitions d'un client sont creees a partir des traitements, via
     * {@link com.minds.rgpd.business.utilities.DefinitionResolver} : elles ne
     * sont pas ecrites en sens inverse. {@link Definition} etant abstraite,
     * MapStruct ne saurait de toute facon pas l'instancier.
     */
    @Mapping(target = "definitions", ignore = true)
    Client map(ClientDTO clientDTO);

    List<Client> mapToClientList(List<ClientDTO> clientsDTO);

    ClientDTO map(Client client);

    List<ClientDTO> mapToDTOList(List<Client> clients);

    DureeDTO mapDuree(Duree duree);

    @Mapping(target = "client", ignore = true)
    Duree mapDuree(DureeDTO dureeDTO);

    DefinitionDTO mapDefinition(Definition definition);

    ResponsableTraitementDTO mapResponsableTraitement(ResponsableTraitement responsableTraitement);

    @Mapping(target = "client", ignore = true)
    ResponsableTraitement mapResponsableTraitement(ResponsableTraitementDTO responsableTraitementDTO);
}
