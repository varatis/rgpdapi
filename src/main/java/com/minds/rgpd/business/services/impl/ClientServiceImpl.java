package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ClientWriteDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
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
@Transactional(readOnly = true)
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

    @Override
    @Transactional
    public ClientDTO createClient(ClientWriteDTO payload) {
        verifierNomDisponible(payload.nom(), null);

        Client client = Client.builder()
                .nom(payload.nom())
                .statut(payload.statut())
                .version(payload.version())
                .dateVersion(payload.dateVersion())
                .build();

        Client cree = clientRepository.save(client);
        log.info("Client créé : {} ({})", cree.getNom(), cree.getId());

        return clientMapper.map(cree);
    }

    @Override
    @Transactional
    public ClientDTO updateClient(UUID id, ClientWriteDTO payload) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        verifierNomDisponible(payload.nom(), id);

        client.setNom(payload.nom());
        client.setStatut(payload.statut());
        client.setVersion(payload.version());
        client.setDateVersion(payload.dateVersion());

        return clientMapper.map(clientRepository.save(client));
    }

    private void verifierNomDisponible(String nom, UUID idCourant) {
        clientRepository.findByNom(nom)
                .filter(existant -> !existant.getId().equals(idCourant))
                .ifPresent(existant -> {
                    throw new DuplicateResourceException("Client", "nom", nom);
                });
    }
}
