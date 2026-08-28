package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.utilities.mappers.PreconisationMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.HistorisationPreconisation;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.HistorisationPreconisationRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreconisationServiceImplTest {

    private static final UUID CLIENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TRAITEMENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRECONISATION_UUID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private PreconisationRepository preconisationRepository;

    @Mock
    private HistorisationPreconisationRepository historisationPreconisationRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TraitementRepository traitementRepository;

    @Mock
    private PreconisationMapper preconisationMapper;

    @InjectMocks
    private PreconisationServiceImpl preconisationService;

    private Client client() {
        Client client = new Client();
        client.setId(CLIENT_UUID);
        client.setNom("ClientA");
        return client;
    }

    private Traitement traitement(Client client) {
        Traitement traitement = new Traitement();
        traitement.setIdentifiant(TRAITEMENT_UUID);
        traitement.setNom("Paie");
        traitement.setClient(client);
        return traitement;
    }

    private PreconisationDTO dto(UUID traitementIdentifiant) {
        return PreconisationDTO.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Chiffrer les sauvegardes")
                .priorite("Haute")
                .client(ClientDTO.builder().id(CLIENT_UUID).nom("ClientA").build())
                .traitementIdentifiant(traitementIdentifiant)
                .build();
    }

    @Test
    void createPreconisation_avecClientEtTraitement_sauvegardeEtRetourneLeDTO() {
        // GIVEN
        Client client = client();
        Traitement traitement = traitement(client);
        PreconisationDTO dto = dto(TRAITEMENT_UUID);
        Preconisation mapped = Preconisation.builder().libelle(dto.libelle()).build();
        Preconisation saved = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle(dto.libelle())
                .client(client)
                .traitement(traitement)
                .build();

        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(traitementRepository.findById(TRAITEMENT_UUID)).thenReturn(Optional.of(traitement));
        when(preconisationRepository.findDuplicates(client, dto.libelle(), traitement)).thenReturn(List.of());
        when(preconisationMapper.mapToPreconisation(dto)).thenReturn(mapped);
        when(preconisationRepository.save(any(Preconisation.class))).thenReturn(saved);
        when(preconisationMapper.mapToDTO(saved)).thenReturn(dto);

        // WHEN
        PreconisationDTO resultat = preconisationService.createPreconisation(dto);

        // THEN
        assertThat(resultat).isEqualTo(dto);
        ArgumentCaptor<Preconisation> captor = ArgumentCaptor.forClass(Preconisation.class);
        verify(preconisationRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isEqualTo(client);
        assertThat(captor.getValue().getTraitement()).isEqualTo(traitement);
    }

    @Test
    void createPreconisation_sansTraitement_sauvegardeUnePreconisationGlobale() {
        // GIVEN
        Client client = client();
        PreconisationDTO dto = dto(null);
        Preconisation mapped = Preconisation.builder().libelle(dto.libelle()).build();

        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(preconisationRepository.findDuplicates(client, dto.libelle(), null)).thenReturn(List.of());
        when(preconisationMapper.mapToPreconisation(dto)).thenReturn(mapped);
        when(preconisationRepository.save(any(Preconisation.class))).thenReturn(mapped);
        when(preconisationMapper.mapToDTO(mapped)).thenReturn(dto);

        // WHEN
        PreconisationDTO resultat = preconisationService.createPreconisation(dto);

        // THEN
        assertThat(resultat).isEqualTo(dto);
        verify(traitementRepository, never()).findById(any());
        ArgumentCaptor<Preconisation> captor = ArgumentCaptor.forClass(Preconisation.class);
        verify(preconisationRepository).save(captor.capture());
        assertThat(captor.getValue().getTraitement()).isNull();
    }

    @Test
    void createPreconisation_enDoublon_leveDuplicateResourceException() {
        // GIVEN
        Client client = client();
        PreconisationDTO dto = dto(null);
        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(preconisationRepository.findDuplicates(client, dto.libelle(), null))
                .thenReturn(List.of(Preconisation.builder().libelle(dto.libelle()).build()));

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.createPreconisation(dto))
                .isInstanceOf(DuplicateResourceException.class);
        verify(preconisationRepository, never()).save(any());
    }

    @Test
    void createPreconisation_clientInconnu_leveResourceNotFoundException() {
        // GIVEN
        PreconisationDTO dto = dto(null);
        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.createPreconisation(dto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preconisationRepository, never()).save(any());
    }

    @Test
    void createPreconisation_traitementDUnAutreClient_leveResourceNotFoundException() {
        // GIVEN
        Client client = client();
        Client autreClient = new Client();
        autreClient.setId(UUID.fromString("00000000-0000-0000-0000-000000000009"));
        Traitement traitement = traitement(autreClient);
        PreconisationDTO dto = dto(TRAITEMENT_UUID);

        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(traitementRepository.findById(TRAITEMENT_UUID)).thenReturn(Optional.of(traitement));

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.createPreconisation(dto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preconisationRepository, never()).save(any());
    }

    @Test
    void updatePreconisation_existante_reporteLesChampsEtSauvegarde() {
        // GIVEN
        Client client = client();
        Traitement traitement = traitement(client);
        PreconisationDTO dto = dto(TRAITEMENT_UUID);
        Preconisation existante = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Ancien libellé")
                .client(client)
                .build();

        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.of(existante));
        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(traitementRepository.findById(TRAITEMENT_UUID)).thenReturn(Optional.of(traitement));
        when(preconisationRepository.save(existante)).thenReturn(existante);
        when(preconisationMapper.mapToDTO(existante)).thenReturn(dto);

        // WHEN
        PreconisationDTO resultat = preconisationService.updatePreconisation(PRECONISATION_UUID, dto);

        // THEN
        assertThat(resultat).isEqualTo(dto);
        verify(preconisationMapper).updatePreconisationFromDto(dto, existante);
        assertThat(existante.getClient()).isEqualTo(client);
        assertThat(existante.getTraitement()).isEqualTo(traitement);
        verify(preconisationRepository).save(existante);
    }

    @Test
    void updatePreconisation_inconnue_leveResourceNotFoundException() {
        // GIVEN
        PreconisationDTO dto = dto(null);
        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.updatePreconisation(PRECONISATION_UUID, dto))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preconisationRepository, never()).save(any());
    }

    @Test
    void deletePreconisationById_existante_supprimeLaPreconisation() {
        // GIVEN
        Preconisation existante = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Chiffrer les sauvegardes")
                .build();
        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.of(existante));

        // WHEN
        preconisationService.deletePreconisationById(PRECONISATION_UUID);

        // THEN
        verify(preconisationRepository).delete(eq(existante));
    }

    @Test
    void deletePreconisationById_inconnue_leveResourceNotFoundException() {
        // GIVEN
        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.deletePreconisationById(PRECONISATION_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(preconisationRepository, never()).delete(any(Preconisation.class));
    }

    @Test
    void updatePreconisation_historiseLaModificationAvecLeMotifUtilisateur() {
        // GIVEN
        Client client = client();
        Traitement traitement = traitement(client);
        Preconisation existante = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Ancien libellé")
                .client(client)
                .build();
        PreconisationDTO dto = PreconisationDTO.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Chiffrer les sauvegardes")
                .priorite("Haute")
                .motifModification("Priorité revue au comité")
                .client(ClientDTO.builder().id(CLIENT_UUID).nom("ClientA").build())
                .traitementIdentifiant(TRAITEMENT_UUID)
                .build();

        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.of(existante));
        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(traitementRepository.findById(TRAITEMENT_UUID)).thenReturn(Optional.of(traitement));
        when(preconisationRepository.save(existante)).thenReturn(existante);
        when(preconisationMapper.mapToDTO(existante)).thenReturn(dto);

        // WHEN
        preconisationService.updatePreconisation(PRECONISATION_UUID, dto);

        // THEN (RG1 : la modification est tracée dans historisation_preconisation)
        ArgumentCaptor<HistorisationPreconisation> captor =
                ArgumentCaptor.forClass(HistorisationPreconisation.class);
        verify(historisationPreconisationRepository).save(captor.capture());
        HistorisationPreconisation historisation = captor.getValue();
        assertThat(historisation.getPreconisation()).isEqualTo(existante);
        assertThat(historisation.getMotif()).isEqualTo("Priorité revue au comité");
        assertThat(historisation.getDate()).isNotNull();
    }

    @Test
    void updatePreconisation_sansMotifUtilisateur_historiseAvecLeMotifParDefaut() {
        // GIVEN
        Client client = client();
        Preconisation existante = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Chiffrer les sauvegardes")
                .client(client)
                .build();
        PreconisationDTO dto = dto(null);

        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.of(existante));
        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(preconisationRepository.save(existante)).thenReturn(existante);
        when(preconisationMapper.mapToDTO(existante)).thenReturn(dto);

        // WHEN
        preconisationService.updatePreconisation(PRECONISATION_UUID, dto);

        // THEN (CA4 : un motif est toujours posé, même sans saisie utilisateur)
        ArgumentCaptor<HistorisationPreconisation> captor =
                ArgumentCaptor.forClass(HistorisationPreconisation.class);
        verify(historisationPreconisationRepository).save(captor.capture());
        assertThat(captor.getValue().getMotif()).isEqualTo("Modification de la préconisation");
    }

    @Test
    void createPreconisation_neProduitPasDHistorisation() {
        // GIVEN
        Client client = client();
        PreconisationDTO dto = dto(null);
        Preconisation mapped = Preconisation.builder().libelle(dto.libelle()).build();

        when(clientRepository.findById(CLIENT_UUID)).thenReturn(Optional.of(client));
        when(preconisationRepository.findDuplicates(client, dto.libelle(), null)).thenReturn(List.of());
        when(preconisationMapper.mapToPreconisation(dto)).thenReturn(mapped);
        when(preconisationRepository.save(mapped)).thenReturn(mapped);
        when(preconisationMapper.mapToDTO(mapped)).thenReturn(dto);

        // WHEN
        preconisationService.createPreconisation(dto);

        // THEN (RG1 : seule la modification est historisée)
        verifyNoInteractions(historisationPreconisationRepository);
    }

    @Test
    void getHistoriquePreconisation_retourneLesEntreesDuPlusRecentAuPlusAncien() {
        // GIVEN
        Preconisation preconisation = Preconisation.builder()
                .identifiant(PRECONISATION_UUID)
                .libelle("Chiffrer les sauvegardes")
                .build();
        HistorisationPreconisation entree = HistorisationPreconisation.builder()
                .date(LocalDateTime.of(2026, 8, 25, 15, 30))
                .motif("Priorité revue au comité")
                .preconisation(preconisation)
                .build();

        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.of(preconisation));
        when(historisationPreconisationRepository.findByPreconisationIdentifiantOrderByDateDesc(PRECONISATION_UUID))
                .thenReturn(List.of(entree));

        // WHEN
        List<HistorisationDTO> historique = preconisationService.getHistoriquePreconisation(PRECONISATION_UUID);

        // THEN
        assertThat(historique).hasSize(1);
        assertThat(historique.get(0).motif()).isEqualTo("Priorité revue au comité");
        assertThat(historique.get(0).date()).isEqualTo(LocalDateTime.of(2026, 8, 25, 15, 30));
    }

    @Test
    void getHistoriquePreconisation_inconnue_leveResourceNotFoundException() {
        // GIVEN
        when(preconisationRepository.findById(PRECONISATION_UUID)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> preconisationService.getHistoriquePreconisation(PRECONISATION_UUID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}