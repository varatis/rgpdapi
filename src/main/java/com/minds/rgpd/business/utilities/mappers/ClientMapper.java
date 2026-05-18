package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.persistence.entities.Client;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    Client map(ClientDTO clientDTO);
    List<Client> mapToClientList(List<ClientDTO> clientsDTO);

    ClientDTO map(Client client);
    List<ClientDTO> mapToDTOList(List<Client> clients);

}
