package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.utilities.mappers.PreconisationMapper;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreconisationServiceImplTest {

    private static final UUID FIXED_UUID = UUID.fromString("aaaaaaaa-1111-4111-8111-111111111111");

    @Mock
    private PreconisationRepository preconisationRepository;

    @Mock
    private PreconisationMapper preconisationMapper;

    @InjectMocks
    private PreconisationServiceImpl preconisationService;

    @Test
    void getPreconisations_sansClient_retournePageVideSansAppelRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        PreconisationFilterCriteria criteria = new PreconisationFilterCriteria("DPO", null, null);

        Page<PreconisationPartielDTO> result = preconisationService.getPreconisations(pageable, null, criteria);

        assertThat(result).isEmpty();
        verify(preconisationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getPreconisations_avecClient_passeCriteriaAuRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        PreconisationFilterCriteria criteria = new PreconisationFilterCriteria("DPO", "En cours", FIXED_UUID);
        Page<Preconisation> entityPage = new PageImpl<>(List.of());
        Page<PreconisationPartielDTO> dtoPage = new PageImpl<>(List.of(
                new PreconisationPartielDTO(FIXED_UUID, "Créer une adresse mail DPO", "1 : Très urgent", "1 : Très simple", "En cours", FIXED_UUID, "Gestion des salariés")
        ));

        when(preconisationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(preconisationMapper.toPartielDTOPage(entityPage)).thenReturn(dtoPage);

        Page<PreconisationPartielDTO> result = preconisationService.getPreconisations(pageable, "La breteche", criteria);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().etatAvancement()).isEqualTo("En cours");

        ArgumentCaptor<Specification<Preconisation>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(preconisationRepository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }

    @Test
    void getOnePreconisation_trouvee() {
        Preconisation entity = Preconisation.builder().identifiant(FIXED_UUID).libelle("DPO").etatAvancement("En cours").build();
        PreconisationDTO dto = PreconisationDTO.builder()
                .identifiant(FIXED_UUID)
                .libelle("DPO")
                .etatAvancement("En cours")
                .build();

        when(preconisationRepository.findById(FIXED_UUID)).thenReturn(Optional.of(entity));
        when(preconisationMapper.mapToDTO(entity)).thenReturn(dto);

        PreconisationDTO result = preconisationService.getOnePreconisation(FIXED_UUID);

        assertThat(result.etatAvancement()).isEqualTo("En cours");
        assertThat(result.libelle()).isEqualTo("DPO");
    }

    @Test
    void getOnePreconisation_inconnue_leveException() {
        when(preconisationRepository.findById(FIXED_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preconisationService.getOnePreconisation(FIXED_UUID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Préconisation");
    }
}
