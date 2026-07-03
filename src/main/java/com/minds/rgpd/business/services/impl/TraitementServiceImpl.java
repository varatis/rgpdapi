package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final ClientRepository clientRepository;
    private final TraitementMapper traitementMapper;

    @Override
    public Page<TraitementPartielDTO> getTraitements(Pageable pageable) {
        Page<Traitement> traitementList = traitementRepository.findAll(pageable);
        return traitementMapper.toTraitementPartielDTOPage(traitementList);
    }

    @Override
    public TraitementDTO getOneTraitement(int id) {
        Traitement traitement = traitementRepository.findByIdFonctionnel(id);
        if (traitement == null) {
            throw new ResourceNotFoundException("Traitement", "idFonctionnel", id);
        }
        return traitementMapper.mapToDTO(traitement);
    }

    @Override
    @Transactional
    public TraitementDTO createTraitement(TraitementDTO traitementDTO) {
        Client client = clientRepository.findById(traitementDTO.client().id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", traitementDTO.client().id()));

        List<Traitement> existingTraitements = traitementRepository.findByAllBusinessFields(
                traitementDTO.nom(),
                client,
                traitementDTO.gestionnaire(),
                traitementDTO.finalitePrincipale(),
                traitementDTO.dateIdentification()
        );

        if (!existingTraitements.isEmpty()) {
            throw new DuplicateResourceException("Traitement", "nom", traitementDTO.nom());
        }

        return traitementMapper.mapToDTO(
                traitementRepository.save(traitementMapper.mapToTraitement(traitementDTO))
        );
    }

    @Override
    public Integer getNextIdFonctionnel() {
        return traitementRepository.findMaxIdFonctionnel().orElse(0) + 1;
    }

    @Override
    @Transactional
    public Integer deleteDuplicateTraitements() {
        List<Traitement> duplicates = traitementRepository.findDuplicateTraitements();
        int count = duplicates.size();

        traitementRepository.deleteAllInBatch(duplicates);

        return count;
    }
}