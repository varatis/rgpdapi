package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests du service utilisateurs : le cas « utilisateur sans client »
 * (clientId et groupe null, ex. superadmin) doit passer, et un clientId
 * inconnu doit échouer avant toute mutation Keycloak.
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private IdentityGateway identityGateway;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    /** Un utilisateur sans client est légal : plus de « The given id must not be null ». */
    @Test
    void modifierUnUtilisateurSansClientPasse() {
        UtilisateurWriteDTO payload =
                new UtilisateurWriteDTO("Prénom", "Nom", "admin@alpha.com", List.of("user"), null, null, true);

        when(identityGateway.rolesDisponibles()).thenReturn(List.of("user", "admin"));

        UtilisateurDTO dto = utilisateurService.modifier(UUID.randomUUID(), payload);

        verify(identityGateway).modifierUtilisateur(any(UUID.class), any(IdentiteCommande.class));
        assertThat(dto.clientId()).isNull();
        assertThat(dto.clientNom()).isNull();
        verifyNoInteractions(clientRepository);
    }

    /** Un clientId inconnu échoue avant la mutation Keycloak (pas d'effet partiel). */
    @Test
    void modifierAvecClientIdInconnuEchoueAvantTouteMutation() {
        UUID clientId = UUID.randomUUID();
        UtilisateurWriteDTO payload =
                new UtilisateurWriteDTO("Prénom", "Nom", "admin@alpha.com", List.of("user"), clientId, "Inconnu", true);

        when(identityGateway.rolesDisponibles()).thenReturn(List.of("user", "admin"));
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.modifier(UUID.randomUUID(), payload))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("Client introuvable");

        verify(identityGateway, never()).modifierUtilisateur(any(UUID.class), any(IdentiteCommande.class));
    }

    /** Création avec client renseigné : la réponse porte clientId et clientNom. */
    @Test
    void creerAvecClientRenseigneRenvoieLeClient() {
        UUID clientId = UUID.randomUUID();
        Client client = Client.builder().id(clientId).nom("Dupont").build();
        UtilisateurWriteDTO payload =
                new UtilisateurWriteDTO("Alice", "Dupont", "alice@alpha.com", List.of("user"), clientId, "Dupont", true);

        when(identityGateway.rolesDisponibles()).thenReturn(List.of("user", "admin"));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(identityGateway.creerUtilisateur(any(IdentiteCommande.class))).thenReturn(UUID.randomUUID());

        UtilisateurDTO dto = utilisateurService.creer(payload);

        assertThat(dto.clientId()).isEqualTo(clientId);
        assertThat(dto.clientNom()).isEqualTo("Dupont");
    }
}
