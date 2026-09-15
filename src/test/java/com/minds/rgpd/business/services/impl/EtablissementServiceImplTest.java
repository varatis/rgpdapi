package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceInUseException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.utilities.mappers.EtablissementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EtablissementServiceImplTest {
    @Mock
    private EtablissementMapper etablissementMapper;

    @Mock
    private EtablissementRepository etablissementRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private TraitementRepository traitementRepository;

    @InjectMocks
    private EtablissementServiceImpl etablissementService;

    private final UUID clientId = UUID.randomUUID();

    private Client client() {
        return Client.builder()
                .id(clientId)
                .nom("DUPONT")
                .statut("ACTIF")
                .build();
    }

    private ClientDTO clientDTO() {
        return ClientDTO.builder()
                .id(clientId)
                .nom("DUPONT")
                .statut("ACTIF")
                .build();
    }

    private Etablissement etablissement(UUID id) {
        Etablissement etablissement = new Etablissement();
        etablissement.setId(id);
        etablissement.setNom("CREATIVE");
        etablissement.setDepartement("2A");
        etablissement.setPrincipal(true);
        etablissement.setClient(client());
        return etablissement;
    }

    private EtablissementDTO etablissementDTO(UUID id) {
        return EtablissementDTO.builder()
                .id(id)
                .nom("CREATIVE")
                .departement("2A")
                .principal(true)
                .client(clientDTO())
                .build();
    }

    @Test
    void getEtablissements() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Etablissement> page = new PageImpl<>(List.of(etablissement(uuid)), pageable, 1);
        Page<EtablissementDTO> pageDTO = new PageImpl<>(List.of(etablissementDTO(uuid)), pageable, 1);

        when(etablissementRepository.findAll(ArgumentMatchers.<Specification<Etablissement>>any(), eq(pageable)))
                .thenReturn(page);
        when(etablissementMapper.mapToDTOPage(page)).thenReturn(pageDTO);

        // WHEN
        Page<EtablissementDTO> resultat = etablissementService.getEtablissements(
                pageable, "DUPONT", EtablissementFilterCriteria.empty());

        // THEN
        assertEquals(1, resultat.getTotalElements());
        assertEquals(uuid, resultat.getContent().getFirst().id());
        assertEquals("CREATIVE", resultat.getContent().getFirst().nom());
        assertEquals("2A", resultat.getContent().getFirst().departement());
        assertEquals(clientDTO(), resultat.getContent().getFirst().client());

        verify(etablissementMapper, times(1)).mapToDTOPage(page);
    }

    @Test
    void getEtablissementsSansClientNeListeRien() {

        // GIVEN un appelant dont le jeton ne designe aucun client
        Pageable pageable = PageRequest.of(0, 20);

        // WHEN
        Page<EtablissementDTO> resultat = etablissementService.getEtablissements(pageable, null, null);

        // THEN
        assertTrue(resultat.isEmpty());
        verify(etablissementRepository, never())
                .findAll(ArgumentMatchers.<Specification<Etablissement>>any(), any(Pageable.class));
    }

    @Test
    void createEtablissementAttribueUnIdentifiantEtLeClientDeLaBase() {

        // GIVEN
        Client client = client();
        EtablissementDTO payload = etablissementDTO(null);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(etablissementRepository.existsByNomIgnoreCaseAndClient("CREATIVE", client)).thenReturn(false);
        when(etablissementMapper.map(payload)).thenReturn(etablissement(null));
        when(etablissementRepository.save(any(Etablissement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(etablissementMapper.map(any(Etablissement.class))).thenReturn(payload);

        // WHEN
        etablissementService.createEtablissement(payload);

        // THEN
        ArgumentCaptor<Etablissement> captor = ArgumentCaptor.forClass(Etablissement.class);
        verify(etablissementRepository).save(captor.capture());
        assertNotNull(captor.getValue().getId());
        assertEquals(client, captor.getValue().getClient());
    }

    @Test
    void createEtablissementRefuseUnNomDejaPrisChezLeClient() {

        // GIVEN
        Client client = client();
        EtablissementDTO payload = etablissementDTO(null);

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(etablissementRepository.existsByNomIgnoreCaseAndClient("CREATIVE", client)).thenReturn(true);

        // WHEN / THEN
        assertThrows(DuplicateResourceException.class, () -> etablissementService.createEtablissement(payload));
        verify(etablissementRepository, never()).save(any());
    }

    @Test
    void updateEtablissementConserveIdentifiantEtRattacheLeClient() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = client();
        Etablissement existant = etablissement(uuid);
        EtablissementDTO payload = etablissementDTO(uuid);

        when(etablissementRepository.findById(uuid)).thenReturn(Optional.of(existant));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(etablissementRepository.existsByNomIgnoreCaseAndClientAndIdNot("CREATIVE", client, uuid)).thenReturn(false);
        when(etablissementRepository.save(existant)).thenReturn(existant);
        when(etablissementMapper.map(existant)).thenReturn(payload);

        // WHEN
        EtablissementDTO resultat = etablissementService.updateEtablissement(uuid, payload);

        // THEN
        assertEquals(uuid, resultat.id());
        verify(etablissementMapper, times(1)).updateEtablissementFromDto(payload, existant);
        assertEquals(uuid, existant.getId());
        assertEquals(client, existant.getClient());
    }

    @Test
    void updateEtablissementInconnuRemonteUneErreur() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        EtablissementDTO payload = etablissementDTO(uuid);

        when(etablissementRepository.findById(uuid)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(ResourceNotFoundException.class, () -> etablissementService.updateEtablissement(uuid, payload));
    }

    @Test
    void deleteEtablissementSansTraitement() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Etablissement existant = etablissement(uuid);

        when(etablissementRepository.findById(uuid)).thenReturn(Optional.of(existant));
        when(traitementRepository.countByEtablissementsContains(existant)).thenReturn(0L);

        // WHEN
        etablissementService.deleteEtablissementById(uuid);

        // THEN
        verify(etablissementRepository, times(1)).delete(existant);
    }

    @Test
    void deleteEtablissementRattacheAUnTraitementEstRefuse() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Etablissement existant = etablissement(uuid);

        when(etablissementRepository.findById(uuid)).thenReturn(Optional.of(existant));
        when(traitementRepository.countByEtablissementsContains(existant)).thenReturn(3L);

        // WHEN / THEN
        assertThrows(ResourceInUseException.class, () -> etablissementService.deleteEtablissementById(uuid));
        verify(etablissementRepository, never()).delete(any(Etablissement.class));
    }
}
