package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import com.minds.rgpd.persistence.specifications.TraitementSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TraitementServiceImpl implements TraitementService {

    private final TraitementRepository traitementRepository;
    private final ClientRepository clientRepository;
    private final EtablissementRepository etablissementRepository;
    private final TraitementMapper traitementMapper;

    @Override
    public Page<TraitementPartielDTO> getTraitements(Pageable pageable, String clientName, TraitementFilterCriteria criteria) {
        if (clientName == null || clientName.isBlank()) {
            return Page.empty(pageable);
        }

        TraitementFilterCriteria safe = criteria != null ? criteria : TraitementFilterCriteria.empty();
        Specification<Traitement> spec = TraitementSpecifications.search(
                clientName,
                safe.nom(),
                safe.gestionnaireMiseEnOeuvre(),
                safe.finalitePrincipale()
        );

        Page<Traitement> traitementList = traitementRepository.findAll(spec, pageable);
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
                traitementDTO.gestionnaireMiseEnOeuvre(),
                traitementDTO.finalitePrincipale(),
                traitementDTO.dateIdentification()
        );

        if (!existingTraitements.isEmpty()) {
            throw new DuplicateResourceException("Traitement", "nom", traitementDTO.nom());
        }

        List<Etablissement> etablissements = resolveEtablissements(traitementDTO.etablissements(), client);

        Traitement traitement = traitementMapper.mapToTraitement(traitementDTO);
        traitement.setEtablissements(etablissements);

        return traitementMapper.mapToDTO(traitementRepository.save(traitement));
    }

    @Override
    @Transactional
    public TraitementDTO updateTraitement(int idFonctionnel, TraitementDTO traitementDTO) {
        Traitement traitement = traitementRepository.findByIdFonctionnel(idFonctionnel);
        if (traitement == null) {
            throw new ResourceNotFoundException("Traitement", "idFonctionnel", idFonctionnel);
        }

        Client client = clientRepository.findById(traitementDTO.client().id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", traitementDTO.client().id()));

        List<Etablissement> etablissements = resolveEtablissements(traitementDTO.etablissements(), client);

        traitementMapper.updateTraitementFromDto(traitementDTO, traitement);
        traitement.setEtablissements(etablissements);

        return traitementMapper.mapToDTO(traitementRepository.save(traitement));
    }

    private List<Etablissement> resolveEtablissements(List<EtablissementDTO> etablissementDTOs, Client client) {
        if (etablissementDTOs == null || etablissementDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        return etablissementDTOs.stream()
                .map(dto -> findExistingEtablissement(dto, client)
                        .orElseGet(() -> createEtablissement(dto, client)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Optional<Etablissement> findExistingEtablissement(EtablissementDTO dto, Client client) {
        if (dto.id() != null) {
            return etablissementRepository.findById(dto.id());
        }
        return etablissementRepository.findByNomAndClient(dto.nom(), client);
    }

    private Etablissement createEtablissement(EtablissementDTO dto, Client client) {
        Etablissement etablissement = Etablissement.builder()
                .id(UUID.randomUUID())
                .nom(dto.nom())
                .client(client)
                .build();

        return etablissementRepository.save(etablissement);
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

    @Override
    @Transactional
    public void deleteTraitementById(UUID id) {
        Traitement traitement = traitementRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Traitement", "UUID", id));
        traitementRepository.delete(traitement);
    }
}