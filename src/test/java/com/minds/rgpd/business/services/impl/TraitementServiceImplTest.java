package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.utilities.DefinitionResolver;
import com.minds.rgpd.business.utilities.DureeResolver;
import com.minds.rgpd.business.utilities.ResponsableTraitementResolver;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.HistorisationTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.HistorisationTraitementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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
class TraitementServiceImplTest {

    private static final UUID FIXED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private TraitementRepository traitementRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EtablissementRepository etablissementRepository;

    @Mock
    private HistorisationTraitementRepository historisationTraitementRepository;

    @Mock
    private TraitementMapper traitementMapper;

    @Mock
    private DefinitionResolver definitionResolver;

    @Mock
    private DureeResolver dureeResolver;

    @Mock
    private ResponsableTraitementResolver responsableTraitementResolver;

    @InjectMocks
    private TraitementServiceImpl traitementService;

    @Test
    void getTraitements_sansClient_retournePageVideSansAppelRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        TraitementFilterCriteria criteria = new TraitementFilterCriteria("nom", null, null);

        Page<TraitementPartielDTO> result = traitementService.getTraitements(pageable, null, criteria);

        assertThat(result).isEmpty();
        verify(traitementRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void updateTraitement_historiseLaModificationAvecLeMotifUtilisateur() {
        // GIVEN
        Client client = client();
        TraitementDTO dto = dto("Correction du nom");
        Traitement existant = new Traitement();
        existant.setIdentifiant(FIXED_UUID);
        existant.setIdFonctionnel(1);

        when(traitementRepository.findByIdFonctionnel(1)).thenReturn(existant);
        when(clientRepository.findById(FIXED_UUID)).thenReturn(Optional.of(client));
        when(traitementMapper.mapToTraitement(dto)).thenReturn(new Traitement());
        when(traitementRepository.save(existant)).thenReturn(existant);
        when(traitementMapper.mapToDTO(existant)).thenReturn(dto);

        // WHEN
        traitementService.updateTraitement(1, dto);

        // THEN (RG1 : la modification est tracée dans historisation_traitement)
        ArgumentCaptor<HistorisationTraitement> captor = ArgumentCaptor.forClass(HistorisationTraitement.class);
        verify(historisationTraitementRepository).save(captor.capture());
        HistorisationTraitement historisation = captor.getValue();
        assertThat(historisation.getTraitement()).isEqualTo(existant);
        assertThat(historisation.getMotif()).isEqualTo("Correction du nom");
        assertThat(historisation.getDate()).isNotNull();
    }

    @Test
    void updateTraitement_sansMotifUtilisateur_historiseAvecLeMotifParDefaut() {
        // GIVEN
        Client client = client();
        TraitementDTO dto = dto(null);
        Traitement existant = new Traitement();
        existant.setIdentifiant(FIXED_UUID);

        when(traitementRepository.findByIdFonctionnel(1)).thenReturn(existant);
        when(clientRepository.findById(FIXED_UUID)).thenReturn(Optional.of(client));
        when(traitementMapper.mapToTraitement(dto)).thenReturn(new Traitement());
        when(traitementRepository.save(existant)).thenReturn(existant);
        when(traitementMapper.mapToDTO(existant)).thenReturn(dto);

        // WHEN
        traitementService.updateTraitement(1, dto);

        // THEN (CA4 : un motif est toujours posé, même sans saisie utilisateur)
        ArgumentCaptor<HistorisationTraitement> captor = ArgumentCaptor.forClass(HistorisationTraitement.class);
        verify(historisationTraitementRepository).save(captor.capture());
        assertThat(captor.getValue().getMotif()).isEqualTo("Modification du traitement");
    }

    @Test
    void createTraitement_neProduitPasDHistorisation() {
        // GIVEN
        Client client = client();
        TraitementDTO dto = dto(null);
        Traitement nouveau = new Traitement();

        when(clientRepository.findById(FIXED_UUID)).thenReturn(Optional.of(client));
        when(traitementRepository.findByAllBusinessFields(any(), eq(client), any(), any(), any())).thenReturn(List.of());
        when(traitementMapper.mapToTraitement(dto)).thenReturn(nouveau);
        when(traitementRepository.save(nouveau)).thenReturn(nouveau);
        when(traitementMapper.mapToDTO(nouveau)).thenReturn(dto);

        // WHEN
        traitementService.createTraitement(dto);

        // THEN (RG1 : seule la modification est historisée)
        verifyNoInteractions(historisationTraitementRepository);
    }

    @Test
    void getHistoriqueTraitement_retourneLesEntreesDuPlusRecentAuPlusAncien() {
        // GIVEN
        Traitement traitement = new Traitement();
        traitement.setIdentifiant(FIXED_UUID);
        HistorisationTraitement entree = HistorisationTraitement.builder()
                .date(LocalDateTime.of(2026, 8, 20, 10, 0))
                .motif("Correction du nom")
                .traitement(traitement)
                .build();

        when(traitementRepository.findByIdFonctionnel(1)).thenReturn(traitement);
        when(historisationTraitementRepository.findByTraitementIdentifiantOrderByDateDesc(FIXED_UUID))
                .thenReturn(List.of(entree));

        // WHEN
        List<HistorisationDTO> historique = traitementService.getHistoriqueTraitement(1);

        // THEN
        assertThat(historique).hasSize(1);
        assertThat(historique.get(0).motif()).isEqualTo("Correction du nom");
        assertThat(historique.get(0).date()).isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
    }

    @Test
    void getHistoriqueTraitement_inconnu_leveResourceNotFoundException() {
        // GIVEN
        when(traitementRepository.findByIdFonctionnel(99)).thenReturn(null);

        // WHEN / THEN
        assertThatThrownBy(() -> traitementService.getHistoriqueTraitement(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Client client() {
        Client client = new Client();
        client.setId(FIXED_UUID);
        client.setNom("La breteche");
        return client;
    }

    private TraitementDTO dto(String motifModification) {
        return TraitementDTO.builder()
                .identifiant(FIXED_UUID)
                .idFonctionnel(1)
                .nom("Traitement modifié")
                .dateIdentification(LocalDate.now())
                .client(ClientDTO.builder().id(FIXED_UUID).nom("La breteche").build())
                .motifModification(motifModification)
                .build();
    }
}
