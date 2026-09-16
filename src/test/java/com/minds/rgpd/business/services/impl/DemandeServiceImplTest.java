package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.business.enums.DemandeStatut;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.utilities.mappers.DemandeMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Demande;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.DemandeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeServiceImplTest {

    @Mock
    private DemandeRepository demandeRepository;

    @Mock
    private DemandeMapper demandeMapper;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private DemandeServiceImpl demandeService;

    private final UUID clientId = UUID.randomUUID();
    private final UUID demandeId = UUID.randomUUID();

    private Client client() {
        return Client.builder()
                .id(clientId)
                .nom("DUPONT")
                .statut("ACTIF")
                .build();
    }

    private Demande demande() {
        return Demande.builder()
                .id(demandeId)
                .typeDemande("DROIT_ACCES")
                .descriptionSynthetique("Demande initiale")
                .dateReception(LocalDate.of(2026, 1, 12))
                .statut(DemandeStatut.EN_ATTENTE)
                .client(client())
                .build();
    }

    private DemandeDTO demandeDTO() {
        return DemandeDTO.builder()
                .id(demandeId)
                .typeDemande("DROIT_ACCES")
                .descriptionSynthetique("Description modifiee")
                .clientId(clientId)
                .build();
    }

    @Test
    void updateDemandeRattacheLeClientDeLaBase() {

        // GIVEN
        Demande demande = demande();
        DemandeDTO dto = demandeDTO();

        when(demandeRepository.findById(demandeId)).thenReturn(Optional.of(demande));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client()));
        when(demandeRepository.save(demande)).thenReturn(demande);
        when(demandeMapper.map(demande)).thenReturn(dto);

        // WHEN
        DemandeDTO resultat = demandeService.updateDemande(demandeId, dto);

        // THEN
        assertEquals(dto, resultat);
        assertEquals(clientId, demande.getClient().getId());
        verify(demandeMapper, times(1)).updateDemandeFromDto(dto, demande);
        verify(demandeRepository, times(1)).save(demande);
    }

    @Test
    void updateDemandeSansClientIdConserveLeRattachement() {

        // GIVEN un corps de requete qui ne rappelle pas le client
        Demande demande = demande();
        DemandeDTO dto = DemandeDTO.builder()
                .descriptionSynthetique("Description modifiee")
                .build();

        when(demandeRepository.findById(demandeId)).thenReturn(Optional.of(demande));
        when(demandeRepository.save(demande)).thenReturn(demande);
        when(demandeMapper.map(demande)).thenReturn(dto);

        // WHEN
        demandeService.updateDemande(demandeId, dto);

        // THEN
        assertEquals(clientId, demande.getClient().getId());
        verify(clientRepository, never()).findById(any());
    }

    @Test
    void updateDemandeInconnueLeveResourceNotFound() {

        // GIVEN
        when(demandeRepository.findById(demandeId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(ResourceNotFoundException.class,
                () -> demandeService.updateDemande(demandeId, demandeDTO()));
        verify(demandeRepository, never()).save(any(Demande.class));
    }

    @Test
    void updateDemandeAvecClientInconnuLeveResourceNotFound() {

        // GIVEN
        when(demandeRepository.findById(demandeId)).thenReturn(Optional.of(demande()));
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(ResourceNotFoundException.class,
                () -> demandeService.updateDemande(demandeId, demandeDTO()));
        verify(demandeRepository, never()).save(any(Demande.class));
    }

    @Test
    void deleteDemandeByIdSupprimeLaDemande() {

        // GIVEN
        Demande demande = demande();
        when(demandeRepository.findById(demandeId)).thenReturn(Optional.of(demande));

        // WHEN
        demandeService.deleteDemandeById(demandeId);

        // THEN
        verify(demandeRepository, times(1)).delete(demande);
    }

    @Test
    void deleteDemandeInconnueLeveResourceNotFound() {

        // GIVEN
        when(demandeRepository.findById(demandeId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(ResourceNotFoundException.class,
                () -> demandeService.deleteDemandeById(demandeId));
        verify(demandeRepository, never()).delete(any(Demande.class));
    }
}
