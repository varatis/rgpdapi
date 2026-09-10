package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ClientWriteDTO;
import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void getClients() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = new Client();
        client.setId(uuid);
        client.setNom("Dupont");
        client.setStatut("ACTIF");

        ClientDTO clientDTO = ClientDTO.builder()
                .id(uuid)
                .nom("Dupont")
                .statut("ACTIF")
                .build();

        List<Client> clientListe = List.of(client);
        List<ClientDTO> clientDTOListe = List.of(clientDTO);

        when(clientRepository.findAll()).thenReturn(clientListe);
        when(clientMapper.mapToDTOList(clientListe)).thenReturn(clientDTOListe);

        // WHEN
        List<ClientDTO> resultat = clientService.getClients();

        // THEN
        assertEquals(1, resultat.size());
        assertEquals(uuid, resultat.getFirst().id());
        assertEquals("Dupont", resultat.getFirst().nom());
        assertEquals("ACTIF", resultat.getFirst().statut());

        verify(clientRepository, times(1)).findAll();
        verify(clientMapper, times(1)).mapToDTOList(clientListe);
    }

    @Test
    void creerClient() {

        // GIVEN
        ClientWriteDTO payload =
                new ClientWriteDTO("Dupont", "ACTIF", "3.25", LocalDate.of(2026, 1, 15));

        when(clientRepository.findByNom("Dupont")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientMapper.map(any(Client.class))).thenReturn(ClientDTO.builder().nom("Dupont").build());

        // WHEN
        ClientDTO resultat = clientService.createClient(payload);

        // THEN
        assertEquals("Dupont", resultat.nom());

        ArgumentCaptor<Client> capteur = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(capteur.capture());
        assertEquals("Dupont", capteur.getValue().getNom());
        assertEquals("ACTIF", capteur.getValue().getStatut());
        assertEquals("3.25", capteur.getValue().getVersion());
        assertEquals(LocalDate.of(2026, 1, 15), capteur.getValue().getDateVersion());
    }

    @Test
    void creerClientNomDejaPris() {

        // GIVEN
        ClientWriteDTO payload = new ClientWriteDTO("Dupont", "ACTIF", null, null);
        Client existant = Client.builder().id(UUID.randomUUID()).nom("Dupont").build();

        when(clientRepository.findByNom("Dupont")).thenReturn(Optional.of(existant));

        // WHEN / THEN
        assertThrows(DuplicateResourceException.class, () -> clientService.createClient(payload));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void majClient() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = Client.builder().id(uuid).nom("Dupont").statut("ACTIF").build();
        ClientWriteDTO payload =
                new ClientWriteDTO("Durand", "ARCHIVE", "4.0", LocalDate.of(2026, 3, 1));

        when(clientRepository.findById(uuid)).thenReturn(Optional.of(client));
        when(clientRepository.findByNom("Durand")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientMapper.map(any(Client.class))).thenReturn(ClientDTO.builder().nom("Durand").build());

        // WHEN
        ClientDTO resultat = clientService.updateClient(uuid, payload);

        // THEN
        assertEquals("Durand", resultat.nom());
        assertEquals("Durand", client.getNom());
        assertEquals("ARCHIVE", client.getStatut());
        assertEquals("4.0", client.getVersion());
    }

    /** Enregistrer un client sans changer son nom ne doit pas le voir comme son propre doublon. */
    @Test
    void majClientConserveSonPropreNom() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = Client.builder().id(uuid).nom("Dupont").statut("ACTIF").build();
        ClientWriteDTO payload = new ClientWriteDTO("Dupont", "ARCHIVE", null, null);

        when(clientRepository.findById(uuid)).thenReturn(Optional.of(client));
        when(clientRepository.findByNom("Dupont")).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientMapper.map(any(Client.class))).thenReturn(ClientDTO.builder().nom("Dupont").build());

        // WHEN
        clientService.updateClient(uuid, payload);

        // THEN
        assertEquals("ARCHIVE", client.getStatut());
        verify(clientRepository, times(1)).save(client);
    }

    @Test
    void majClientNomDejaPrisParUnAutre() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = Client.builder().id(uuid).nom("Dupont").build();
        Client autre = Client.builder().id(UUID.randomUUID()).nom("Durand").build();
        ClientWriteDTO payload = new ClientWriteDTO("Durand", "ACTIF", null, null);

        when(clientRepository.findById(uuid)).thenReturn(Optional.of(client));
        when(clientRepository.findByNom("Durand")).thenReturn(Optional.of(autre));

        // WHEN / THEN
        assertThrows(DuplicateResourceException.class, () -> clientService.updateClient(uuid, payload));
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void majClientInconnu() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        ClientWriteDTO payload = new ClientWriteDTO("Dupont", "ACTIF", null, null);

        when(clientRepository.findById(uuid)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(ResourceNotFoundException.class, () -> clientService.updateClient(uuid, payload));
    }
}