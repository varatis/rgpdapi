package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ClientDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ClientService {

    List<ClientDTO> getClients();

    ClientDTO getClientByNom(String nom);

    ClientDTO getClientById(UUID id);
}
