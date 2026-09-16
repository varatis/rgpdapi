package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.business.enums.DemandeStatut;
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
import java.util.Objects;
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

        return demandeMapper.map(findDemande(id));
    }

    @Override
    @Transactional
    public DemandeDTO createDemande(DemandeDTO demandeDTO) {

        Client client = resolveClient(demandeDTO.clientId());

        Demande demande = demandeMapper.map(demandeDTO);

        demande.setClient(client);

        Demande savedDemande =
                demandeRepository.save(demande);

        return demandeMapper.map(savedDemande);
    }

    @Override
    @Transactional
    public DemandeDTO updateDemande(UUID id, DemandeDTO demandeDTO) {

        Demande demande = findDemande(id);

        // Le client n'est réaffecté que s'il est transmis : une modification de
        // contenu n'a pas à rappeler le rattachement pour le conserver.
        if (Objects.nonNull(demandeDTO.clientId())) {
            demande.setClient(resolveClient(demandeDTO.clientId()));
        }

        demandeMapper.updateDemandeFromDto(demandeDTO, demande);

        return demandeMapper.map(demandeRepository.save(demande));
    }

    @Override
    @Transactional
    public void deleteDemandeById(UUID id) {

        demandeRepository.delete(findDemande(id));
    }

    @Transactional
    @Override
    public DemandeDTO traiterDemande(UUID id){
        Demande demande = findDemande(id);
        demande.setStatut(DemandeStatut.TRAITEE);
        Demande savedDemande = demandeRepository.save(demande);
        return demandeMapper.map(savedDemande);
    }

    private Demande findDemande(UUID id) {
        return demandeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Demande", "id", id));
    }

    private Client resolveClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Client", "id", clientId));
    }
}
