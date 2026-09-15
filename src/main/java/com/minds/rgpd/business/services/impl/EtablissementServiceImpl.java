package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceInUseException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.EtablissementService;
import com.minds.rgpd.business.utilities.mappers.EtablissementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import com.minds.rgpd.persistence.specifications.EtablissementSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Default for all read operations
public class EtablissementServiceImpl implements EtablissementService {

    private final EtablissementRepository etablissementRepository;
    private final ClientRepository clientRepository;
    private final TraitementRepository traitementRepository;
    private final EtablissementMapper etablissementMapper;

    @Override
    public Page<EtablissementDTO> getEtablissements(Pageable pageable, String clientNom, EtablissementFilterCriteria criteria) {
        // Le client provient du jeton de l'appelant : sans lui, rien a lister.
        if (clientNom == null || clientNom.isBlank()) {
            return Page.empty(pageable);
        }

        Specification<Etablissement> spec = EtablissementSpecifications.search(clientNom, criteria);

        return etablissementMapper.mapToDTOPage(etablissementRepository.findAll(spec, pageable));
    }

    @Override
    public EtablissementDTO getOneEtablissement(UUID id) {
        return etablissementMapper.map(findEtablissement(id));
    }

    @Override
    @Transactional
    public EtablissementDTO createEtablissement(EtablissementDTO etablissementDTO) {
        Client client = resolveClient(etablissementDTO);

        if (etablissementRepository.existsByNomIgnoreCaseAndClient(etablissementDTO.nom(), client)) {
            throw new DuplicateResourceException("Etablissement", "nom", etablissementDTO.nom());
        }

        Etablissement etablissement = etablissementMapper.map(etablissementDTO);
        // L'identifiant n'est pas genere par la base : un POST qui en transmet un
        // ferait basculer save() sur un merge, ecrasant un etablissement existant.
        etablissement.setId(UUID.randomUUID());
        // Le client issu du DTO est detache : on lui substitue celui charge en base.
        etablissement.setClient(client);

        return etablissementMapper.map(etablissementRepository.save(etablissement));
    }

    @Override
    @Transactional
    public EtablissementDTO updateEtablissement(UUID id, EtablissementDTO etablissementDTO) {
        Etablissement etablissement = findEtablissement(id);
        Client client = resolveClient(etablissementDTO);

        if (etablissementRepository.existsByNomIgnoreCaseAndClientAndIdNot(etablissementDTO.nom(), client, id)) {
            throw new DuplicateResourceException("Etablissement", "nom", etablissementDTO.nom());
        }

        etablissementMapper.updateEtablissementFromDto(etablissementDTO, etablissement);
        etablissement.setClient(client);

        return etablissementMapper.map(etablissementRepository.save(etablissement));
    }

    @Override
    @Transactional
    public void deleteEtablissementById(UUID id) {
        Etablissement etablissement = findEtablissement(id);

        // Les tables de liaison sont en ON DELETE CASCADE : sans ce garde-fou, la
        // suppression retirerait aussi l'etablissement des traitements qui le citent.
        long traitementsRattaches = traitementRepository.countByEtablissementsContains(etablissement);
        if (traitementsRattaches > 0) {
            throw new ResourceInUseException("Etablissement", etablissement.getNom(),
                    traitementsRattaches + " traitement(s)");
        }

        etablissementRepository.delete(etablissement);
    }

    private Etablissement findEtablissement(UUID id) {
        return etablissementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Etablissement", "id", id));
    }

    private Client resolveClient(EtablissementDTO etablissementDTO) {
        if (Objects.isNull(etablissementDTO.client()) || Objects.isNull(etablissementDTO.client().id())) {
            throw new ResourceNotFoundException("Client", "id", null);
        }
        return clientRepository.findById(etablissementDTO.client().id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", etablissementDTO.client().id()));
    }
}
