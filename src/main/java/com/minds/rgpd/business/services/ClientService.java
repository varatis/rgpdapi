package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ClientWriteDTO;
import java.util.List;
import java.util.UUID;

public interface ClientService {

    List<ClientDTO> getClients();

    ClientDTO getClientByNom(String nom);

    ClientDTO getClientById(UUID id);

    ClientDTO createClient(ClientWriteDTO payload);

    ClientDTO updateClient(UUID id, ClientWriteDTO payload);

    void deleteClient(UUID id);
}