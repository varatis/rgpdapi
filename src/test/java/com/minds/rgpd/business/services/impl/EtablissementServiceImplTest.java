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
        Client client = new Client(1, "DUPONT", "ACTIF");
        ClientDTO clientDTO = new ClientDTO(1, "DUPONT", "ACTIF");

        Etablissement etablissement = new Etablissement();
        etablissement.setId(1);
        etablissement.setNom("CREATIVE");
        etablissement.setClient(client);

        EtablissementDTO etablissementDTO = EtablissementDTO.builder()
                .id(1)
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
        assertEquals(1, resultat.getFirst().id());
        assertEquals("CREATIVE", resultat.getFirst().nom());
        assertEquals(clientDTO, resultat.getFirst().client());

        verify(etablissementRepository, times(1)).findAll();
        verify(etablissementMapper, times(1)).mapToDTOList(etablissementListe);
    }
}
