package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.persistence.entities.EtudeImpact;
import com.minds.rgpd.persistence.entities.LiceiteTraitement;
import com.minds.rgpd.persistence.entities.Sensibilite;
import com.minds.rgpd.persistence.repositories.DefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefinitionServiceImplTest {

    @Mock
    private DefinitionRepository definitionRepository;

    @InjectMocks
    private DefinitionServiceImpl definitionService;

    @Test
    void mapsUrlKeyToDiscriminator() {
        when(definitionRepository.findValuesByClientNomAndType("ClientA", Sensibilite.TYPE))
                .thenReturn(List.of("Faible", "Haute"));

        assertThat(definitionService.getValues("ClientA", "sensibilite")).containsExactly("Faible", "Haute");

        definitionService.getValues("ClientA", "etude-impact");
        definitionService.getValues("ClientA", "liceite-traitement");
        verify(definitionRepository).findValuesByClientNomAndType("ClientA", EtudeImpact.TYPE);
        verify(definitionRepository).findValuesByClientNomAndType("ClientA", LiceiteTraitement.TYPE);
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> definitionService.getValues("ClientA", "finalite-principale"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(definitionRepository, never()).findValuesByClientNomAndType(anyString(), anyString());
    }
}
