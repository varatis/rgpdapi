package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.PreconisationService;
import com.minds.rgpd.business.utilities.mappers.PreconisationMapper;
import com.minds.rgpd.persistence.entities.HistorisationPreconisation;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.repositories.HistorisationPreconisationRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.specifications.PreconisationSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PreconisationServiceImpl implements PreconisationService {

    /**
     * RG1 : motif posé sur l'entrée d'historisation lorsque l'utilisateur n'en
     * saisit pas à la modification.
     */
    private static final String MOTIF_MODIFICATION_PAR_DEFAUT = "Modification de la préconisation";

    private final PreconisationRepository preconisationRepository;
    private final HistorisationPreconisationRepository historisationPreconisationRepository;
    private final PreconisationMapper preconisationMapper;
    private final ClientRepository clientRepository;
    private final TraitementRepository traitementRepository;

    @Override
    public Page<PreconisationPartielDTO> getPreconisations(
            Pageable pageable,
            String clientNom,
            PreconisationFilterCriteria criteria
    ) {
        if (clientNom == null || clientNom.isBlank()) {
            return Page.empty(pageable);
        }

        PreconisationFilterCriteria safe = criteria != null ? criteria : PreconisationFilterCriteria.empty();
        Specification<Preconisation> spec = PreconisationSpecifications.search(
                clientNom,
                safe.libelle(),
                safe.etatAvancement(),
                safe.idTraitement()
        );

        Page<Preconisation> page = preconisationRepository.findAll(spec, pageable);
        return preconisationMapper.toPartielDTOPage(page);
    }

    @Override
    public PreconisationDTO getOnePreconisation(UUID id) {
        Preconisation preconisation = preconisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préconisation", "id", id));
        return preconisationMapper.mapToDTO(preconisation);
    }

    @Override
    @Transactional
    public PreconisationDTO createPreconisation(PreconisationDTO preconisationDTO) {
        Client client = resolveClient(preconisationDTO);
        Traitement traitement = resolveTraitement(preconisationDTO, client);

        if (!preconisationRepository.findDuplicates(client, preconisationDTO.libelle(), traitement).isEmpty()) {
            throw new DuplicateResourceException("Préconisation", "libelle", preconisationDTO.libelle());
        }

        Preconisation preconisation = preconisationMapper.mapToPreconisation(preconisationDTO);
        preconisation.setClient(client);
        preconisation.setTraitement(traitement);

        return preconisationMapper.mapToDTO(preconisationRepository.save(preconisation));
    }

    @Override
    @Transactional
    public PreconisationDTO updatePreconisation(UUID id, PreconisationDTO preconisationDTO) {
        Preconisation preconisation = preconisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préconisation", "id", id));

        Client client = resolveClient(preconisationDTO);
        Traitement traitement = resolveTraitement(preconisationDTO, client);

        preconisationMapper.updatePreconisationFromDto(preconisationDTO, preconisation);
        preconisation.setClient(client);
        preconisation.setTraitement(traitement);

        Preconisation sauvegardee = preconisationRepository.save(preconisation);

        // RG1 : toute modification d'une préconisation est historisée. Le motif
        // est renseigné par l'utilisateur (CA4) ; à défaut, un motif générique.
        historiserModification(sauvegardee, preconisationDTO.motifModification());

        return preconisationMapper.mapToDTO(sauvegardee);
    }

    @Override
    public List<HistorisationDTO> getHistoriquePreconisation(UUID id) {
        Preconisation preconisation = preconisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préconisation", "id", id));
        return historisationPreconisationRepository
                .findByPreconisationIdentifiantOrderByDateDesc(preconisation.getIdentifiant())
                .stream()
                .map(historisation -> new HistorisationDTO(historisation.getDate(), historisation.getMotif()))
                .toList();
    }

    private void historiserModification(Preconisation preconisation, String motif) {
        HistorisationPreconisation historisation = HistorisationPreconisation.builder()
                .date(LocalDateTime.now())
                .motif(StringUtils.hasText(motif) ? motif : MOTIF_MODIFICATION_PAR_DEFAUT)
                .preconisation(preconisation)
                .build();
        historisationPreconisationRepository.save(historisation);
    }

    @Override
    @Transactional
    public void deletePreconisationById(UUID id) {
        Preconisation preconisation = preconisationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préconisation", "id", id));
        preconisationRepository.delete(preconisation);
    }

    private Client resolveClient(PreconisationDTO preconisationDTO) {
        if (preconisationDTO.client() == null || preconisationDTO.client().id() == null) {
            throw new ResourceNotFoundException("Client", "id", null);
        }
        return clientRepository.findById(preconisationDTO.client().id())
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", preconisationDTO.client().id()));
    }

    /**
     * Le rattachement à un traitement est optionnel : une préconisation peut
     * être globale au client. Lorsqu'un identifiant est fourni, le traitement
     * doit exister et appartenir au même client que la préconisation.
     */
    private Traitement resolveTraitement(PreconisationDTO preconisationDTO, Client client) {
        if (preconisationDTO.traitementIdentifiant() == null) {
            return null;
        }
        Traitement traitement = traitementRepository.findById(preconisationDTO.traitementIdentifiant())
                .orElseThrow(() -> new ResourceNotFoundException("Traitement", "UUID", preconisationDTO.traitementIdentifiant()));
        if (traitement.getClient() == null || !client.getId().equals(traitement.getClient().getId())) {
            throw new ResourceNotFoundException("Traitement", "UUID", preconisationDTO.traitementIdentifiant());
        }
        return traitement;
    }
}