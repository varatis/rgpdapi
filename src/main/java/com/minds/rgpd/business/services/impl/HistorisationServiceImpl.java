package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.HistorisationCreationDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.HistorisationService;
import com.minds.rgpd.business.utilities.AccessUserInformation;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.CurrentUser;
import com.minds.rgpd.persistence.entities.HistorisationRegistre;
import com.minds.rgpd.persistence.entities.HistorisationTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.HistorisationRegistreRepository;
import com.minds.rgpd.persistence.repositories.HistorisationTraitementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HistorisationServiceImpl implements HistorisationService {

    private static final String AUTEUR_SYSTEME = "système";

    private final HistorisationTraitementRepository historisationTraitementRepository;
    private final HistorisationRegistreRepository historisationRegistreRepository;
    private final TraitementRepository traitementRepository;
    private final ClientRepository clientRepository;

    @Override
    public List<HistorisationDTO> getHistoriqueTraitement(int idFonctionnel) {
        Traitement traitement = findTraitement(idFonctionnel);
        return historisationTraitementRepository.findByTraitementOrderByDateDesc(traitement)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<HistorisationDTO> getHistoriqueRegistre(String clientNom) {
        Client client = findClient(clientNom);
        return historisationRegistreRepository.findByClientOrderByDateDesc(client)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public HistorisationDTO ajouterHistoriqueTraitement(int idFonctionnel, HistorisationCreationDTO creation) {
        Traitement traitement = findTraitement(idFonctionnel);
        HistorisationTraitement historisation = HistorisationTraitement.builder()
                .traitement(traitement)
                .date(dateOuMaintenant(creation))
                .motif(creation.motif())
                .auteur(auteurCourant())
                .build();
        return toDTO(historisationTraitementRepository.save(historisation));
    }

    @Override
    @Transactional
    public HistorisationDTO ajouterHistoriqueRegistre(String clientNom, HistorisationCreationDTO creation) {
        Client client = findClient(clientNom);
        HistorisationRegistre historisation = HistorisationRegistre.builder()
                .client(client)
                .date(dateOuMaintenant(creation))
                .motif(creation.motif())
                .auteur(auteurCourant())
                .build();
        return toDTO(historisationRegistreRepository.save(historisation));
    }

    @Override
    @Transactional
    public void historiserTraitement(Traitement traitement, String motif) {
        if (Objects.isNull(traitement) || Objects.isNull(motif) || motif.isBlank()) {
            return;
        }
        historisationTraitementRepository.save(HistorisationTraitement.builder()
                .traitement(traitement)
                .date(LocalDateTime.now())
                .motif(motif)
                .auteur(auteurCourant())
                .build());
    }

    @Override
    @Transactional
    public void historiserRegistre(Client client, String motif) {
        if (Objects.isNull(client) || Objects.isNull(motif) || motif.isBlank()) {
            return;
        }
        historisationRegistreRepository.save(HistorisationRegistre.builder()
                .client(client)
                .date(LocalDateTime.now())
                .motif(motif)
                .auteur(auteurCourant())
                .build());
    }

    private LocalDateTime dateOuMaintenant(HistorisationCreationDTO creation) {
        return Objects.isNull(creation.date()) ? LocalDateTime.now() : creation.date();
    }

    private String auteurCourant() {
        CurrentUser utilisateur = AccessUserInformation.getCurrentUser();
        if (Objects.isNull(utilisateur)) {
            return AUTEUR_SYSTEME;
        }
        if (Objects.nonNull(utilisateur.getNom()) && !utilisateur.getNom().isBlank()) {
            return utilisateur.getNom();
        }
        return Objects.nonNull(utilisateur.getIdentifiant()) ? utilisateur.getIdentifiant() : AUTEUR_SYSTEME;
    }

    private Traitement findTraitement(int idFonctionnel) {
        Traitement traitement = traitementRepository.findByIdFonctionnel(idFonctionnel);
        if (Objects.isNull(traitement)) {
            throw new ResourceNotFoundException("Traitement", "idFonctionnel", idFonctionnel);
        }
        return traitement;
    }

    private Client findClient(String nom) {
        return clientRepository.findByNom(nom)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "nom", nom));
    }

    private HistorisationDTO toDTO(HistorisationTraitement historisation) {
        return new HistorisationDTO(
                historisation.getId(),
                historisation.getDate(),
                historisation.getMotif(),
                historisation.getAuteur());
    }

    private HistorisationDTO toDTO(HistorisationRegistre historisation) {
        return new HistorisationDTO(
                historisation.getId(),
                historisation.getDate(),
                historisation.getMotif(),
                historisation.getAuteur());
    }
}
