package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.DemandeService;
import com.minds.rgpd.business.utilities.mappers.DemandeMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Demande;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.DemandeRepository;
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
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final DemandeMapper demandeMapper;
    private final ClientRepository clientRepository;

    @Override
    public List<DemandeDTO> getDemandes() {

        List<Demande> demandes = demandeRepository.findAll();

        return demandeMapper.mapToDTOList(demandes);
    }

    @Override
    public DemandeDTO getDemande(UUID id) {

        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Demande", "id", id));

        return demandeMapper.map(demande);
    }

    @Override
    @Transactional
    public DemandeDTO createDemande(DemandeDTO demandeDTO) {

        Client client = clientRepository.findById(demandeDTO.clientId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client",
                                "id",
                                demandeDTO.clientId()));

        Demande demande = demandeMapper.map(demandeDTO);

        demande.setClient(client);

        Demande savedDemande =
                demandeRepository.save(demande);

        return demandeMapper.map(savedDemande);
    }
}