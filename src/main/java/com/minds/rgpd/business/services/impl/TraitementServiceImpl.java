package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.HistorisationService;
import com.minds.rgpd.business.services.TraitementService;
import com.minds.rgpd.business.utilities.TraitementDiff;
import com.minds.rgpd.business.utilities.DefinitionResolver;
import com.minds.rgpd.business.utilities.DureeResolver;
import com.minds.rgpd.business.utilities.ResponsableTraitementResolver;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final DefinitionResolver definitionResolver;
    private final DureeResolver dureeResolver;
    private final ResponsableTraitementResolver responsableTraitementResolver;
    private final HistorisationService historisationService;

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
                Objects.isNull(traitementDTO.finalitePrincipale()) ? null : traitementDTO.finalitePrincipale().valeur(),
                traitementDTO.dateIdentification()
        );

        if (!existingTraitements.isEmpty()) {
            throw new DuplicateResourceException("Traitement", "nom", traitementDTO.nom());
        }

        List<Etablissement> etablissements = resolveEtablissements(traitementDTO.etablissements(), client);

        Traitement traitement = traitementMapper.mapToTraitement(traitementDTO);
        definitionResolver.resolveDefinitions(traitement, client);
        dureeResolver.resolveDurees(traitement, client);
        responsableTraitementResolver.resolveResponsableTraitement(traitement, client);
        traitement.setEtablissements(etablissements);

        Traitement cree = traitementRepository.save(traitement);
        // RG1 : la création est le premier évènement de la vie du traitement.
        historisationService.historiserTraitement(cree, "Création du traitement");

        return traitementMapper.mapToDTO(cree);
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

        // RG1 : l'état antérieur est photographié avant la mise à jour, pour que
        // le motif d'historisation décrive précisément ce qui a changé.
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitementMapper.updateTraitementFromDto(traitementDTO, traitement);

        // Les références vers le référentiel sont résolues sur un traitement
        // transitoire : les rattacher au traitement managé avant résolution
        // ferait échouer le premier flush déclenché par les résolveurs.
        Traitement referentiels = traitementMapper.mapToTraitement(traitementDTO);
        definitionResolver.resolveDefinitions(referentiels, client);
        dureeResolver.resolveDurees(referentiels, client);
        responsableTraitementResolver.resolveResponsableTraitement(referentiels, client);
        traitementMapper.copierReferentiels(referentiels, traitement);

        traitement.setEtablissements(etablissements);
        // Le registre reste cohérent : la date de mise à jour suit toute modification (CA5).
        traitement.setDateMiseAJour(LocalDate.now());

        Traitement modifie = traitementRepository.save(traitement);

        String motif = TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(modifie));
        historisationService.historiserTraitement(modifie, motif);

        return traitementMapper.mapToDTO(modifie);
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
        // RG1 : la suppression est tracée au niveau du registre, l'historique du
        // traitement disparaissant avec lui (cascade).
        historisationService.historiserRegistre(
                traitement.getClient(),
                "Suppression du traitement n°%s « %s »".formatted(traitement.getIdFonctionnel(), traitement.getNom()));
        traitementRepository.delete(traitement);
    }
}