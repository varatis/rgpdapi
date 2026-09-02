package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.services.HistorisationService;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    private TraitementMapper traitementMapper;

    @Mock
    private HistorisationService historisationService;

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
    void getTraitements_avecClient_passeCriteriaAuRepositoryViaSpecification() {
        Pageable pageable = PageRequest.of(0, 20);
        TraitementFilterCriteria criteria = new TraitementFilterCriteria("Paie", "Dupont", "RH");
        Page<Traitement> entityPage = new PageImpl<>(List.of());
        Page<TraitementPartielDTO> dtoPage = new PageImpl<>(List.of(
                new TraitementPartielDTO(FIXED_UUID, 1, "Paie", "Dupont", "RH")
        ));

        when(traitementRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(entityPage);
        when(traitementMapper.toTraitementPartielDTOPage(entityPage)).thenReturn(dtoPage);

        Page<TraitementPartielDTO> result = traitementService.getTraitements(pageable, "ClientA", criteria);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().nom()).isEqualTo("Paie");

        ArgumentCaptor<Specification<Traitement>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(traitementRepository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }
}
