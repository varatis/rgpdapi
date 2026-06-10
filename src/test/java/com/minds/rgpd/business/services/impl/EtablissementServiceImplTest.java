package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.utilities.mappers.EtablissementMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
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
public class EtablissementServiceImplTest {
    @Mock
    private EtablissementMapper etablissementMapper;

    @Mock
    private EtablissementRepository etablissementRepository;

    @InjectMocks
    private EtablissementServiceImpl etablissementService;

    @Test
    void getEtablissements() {

        // GIVEN
        UUID uuid = UUID.randomUUID();
        Client client = new Client(uuid, "DUPONT", "ACTIF");
        ClientDTO clientDTO = new ClientDTO(uuid, "DUPONT", "ACTIF");

        Etablissement etablissement = new Etablissement();
        etablissement.setId(uuid);
        etablissement.setNom("CREATIVE");
        etablissement.setClient(client);

        EtablissementDTO etablissementDTO = EtablissementDTO.builder()
                .id(uuid)
                .nom("CREATIVE")
                .client(clientDTO)
                .build();

        List<Etablissement> etablissementListe = List.of(etablissement);
        List<EtablissementDTO> etablissementDTOListe = List.of(etablissementDTO);

        when(etablissementRepository.findAll()).thenReturn(etablissementListe);
        when(etablissementMapper.mapToDTOList(etablissementListe)).thenReturn(etablissementDTOListe);

        // WHEN
        List<EtablissementDTO> resultat = etablissementService.getEtablissements();

        // THEN
        assertEquals(1, resultat.size());
        assertEquals(uuid, resultat.getFirst().id());
        assertEquals("CREATIVE", resultat.getFirst().nom());
        assertEquals(clientDTO, resultat.getFirst().client());

        verify(etablissementRepository, times(1)).findAll();
        verify(etablissementMapper, times(1)).mapToDTOList(etablissementListe);
    }
}
