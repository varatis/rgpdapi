package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.ClientService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default for all read operations
public class ClientServiceImpl implements ClientService {

    private final ClientMapper clientMapper;
    private final ClientRepository clientRepository;

    @Override
    public List<ClientDTO> getClients() {
        List<Client> clients = clientRepository.findAll();
        return clientMapper.mapToDTOList(clients);
    }

    @Override
    public ClientDTO getClientByNom(String nom) {
        Client client = clientRepository.findByNom(nom)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "nom", nom));
        return clientMapper.map(client);
    }

    @Override
    public ClientDTO getClientById(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "uuid", id));
        return clientMapper.map(client);
    }
}
