package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        ClientDTO clientDTO = new ClientDTO(
                uuid,
                "Dupont",
                "ACTIF"
        );

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
}