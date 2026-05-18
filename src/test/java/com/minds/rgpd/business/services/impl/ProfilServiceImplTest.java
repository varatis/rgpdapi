package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ProfilDTO;
import com.minds.rgpd.business.utilities.mappers.ProfilMapper;
import com.minds.rgpd.persistence.entities.Profil;
import com.minds.rgpd.persistence.repositories.ProfilRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfilServiceImplTest {
    @Mock
    private ProfilMapper profilMapper;

    @Mock
    private ProfilRepository profilRepository;

    @InjectMocks
    private ProfilServiceImpl profilService;

    @Test
    void getProfils() {

        // GIVEN
        Profil profil = new Profil();
        profil.setId(1);
        profil.setCode("PROFIL_CODE");
        profil.setDescription("DESCRIPTION");

        ProfilDTO profilDTO = new ProfilDTO(
                1,
                "PROFIL_CODE",
                "DESCRIPTION"
        );

        List<Profil> profilListe = List.of(profil);
        List<ProfilDTO> profilDTOListe = List.of(profilDTO);

        when(profilRepository.findAll()).thenReturn(profilListe);
        when(profilMapper.mapToDTOList(profilListe)).thenReturn(profilDTOListe);

        // WHEN
        List<ProfilDTO> resultat = profilService.getProfils();

        // THEN
        assertEquals(1, resultat.size());
        assertEquals(1, resultat.getFirst().id());
        assertEquals("PROFIL_CODE", resultat.getFirst().code());
        assertEquals("DESCRIPTION", resultat.getFirst().description());

        verify(profilRepository, times(1)).findAll();
        verify(profilMapper, times(1)).mapToDTOList(profilListe);
    }
}
