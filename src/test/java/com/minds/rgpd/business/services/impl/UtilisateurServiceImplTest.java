package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
class UtilisateurServiceImplTest {

    private static final UUID CLIENT_ID = UUID.fromString("0e4bf889-fea0-46ac-894d-ca39cbf00359");
    private static final UUID AUTRE_CLIENT_ID = UUID.fromString("82e99259-1bbd-4c1a-b013-7602e27168f3");
    private static final UUID ID_ALICE = UUID.fromString("d6dfd117-8047-4a9a-afca-f5268a38bfcf");
    private static final UUID ID_BOB = UUID.fromString("6a04222b-60f8-434b-bdff-c01ce36fde2f");
    private static final UUID ID_CLAIRE = UUID.fromString("e9048a22-e73d-4b35-b08a-0540c58e7a6f");

    @Mock
    private IdentityGateway identityGateway;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    @Test
    void rechercheSansFiltreRetourneToutTriParNom() {
        when(identityGateway.utilisateurs())
                .thenReturn(List.of(utilisateur(ID_CLAIRE, "Claire", "Martin", null),
                        utilisateur(ID_ALICE, "Alice", "Dupont", "La breteche"),
                        utilisateur(ID_BOB, "Bob", "Duteil", "La breteche")));
        when(clientRepository.findAll()).thenReturn(List.of(client(CLIENT_ID, "La breteche")));

        Page<UtilisateurDTO> page =
                utilisateurService.getUtilisateurs(PageRequest.of(0, 20), UtilisateurFilterCriteria.empty());

        assertThat(page.getContent()).extracting(UtilisateurDTO::nom).containsExactly("Dupont", "Duteil", "Martin");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent().getFirst().clientNom()).isEqualTo("La breteche");
        assertThat(page.getContent().getFirst().clientId()).isEqualTo(CLIENT_ID);
        assertThat(page.getContent().get(2).clientId()).isNull();
    }

    @Test
    void filtreSurLeNomInsensibleALaCasseEtAuxAccents() {
        when(identityGateway.utilisateurs()).thenReturn(List.of(
                utilisateur(ID_ALICE, "Alice", "Dupré", null),
                utilisateur(ID_BOB, "Bob", "Martin", null)));
        when(clientRepository.findAll()).thenReturn(List.of());

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(PageRequest.of(0, 20),
                new UtilisateurFilterCriteria("dupr", null, null));

        assertThat(page.getContent()).extracting(UtilisateurDTO::id).containsExactly(ID_ALICE);
    }

    @Test
    void filtreSurLePrenom() {
        when(identityGateway.utilisateurs()).thenReturn(List.of(
                utilisateur(ID_ALICE, "Alice", "Dupont", null),
                utilisateur(ID_BOB, "Bob", "Martin", null)));
        when(clientRepository.findAll()).thenReturn(List.of());

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(PageRequest.of(0, 20),
                new UtilisateurFilterCriteria(null, "ali", null));

        assertThat(page.getContent()).extracting(UtilisateurDTO::id).containsExactly(ID_ALICE);
    }

    @Test
    void filtreSurLeClientRestreintAuxMembresDuGroupe() {
        Client client = client(CLIENT_ID, "La breteche");
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(clientRepository.findAll()).thenReturn(List.of(client));
        when(identityGateway.membresDuGroupe("La breteche")).thenReturn(List.of(ID_ALICE));
        when(identityGateway.utilisateurs()).thenReturn(List.of(
                utilisateur(ID_ALICE, "Alice", "Dupont", "La breteche"),
                utilisateur(ID_BOB, "Bob", "Martin", "Entreprise Alpha")));

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(PageRequest.of(0, 20),
                new UtilisateurFilterCriteria(null, null, CLIENT_ID));

        assertThat(page.getContent()).extracting(UtilisateurDTO::id).containsExactly(ID_ALICE);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void filtreSurUnClientInconnu() {
        when(clientRepository.findById(AUTRE_CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.getUtilisateurs(PageRequest.of(0, 20),
                new UtilisateurFilterCriteria(null, null, AUTRE_CLIENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client");

        verify(identityGateway, never()).utilisateurs();
    }

    @Test
    void recherchePaginee() {
        when(identityGateway.utilisateurs()).thenReturn(List.of(
                utilisateur(ID_ALICE, "Alice", "A", null),
                utilisateur(ID_BOB, "Bob", "B", null),
                utilisateur(ID_CLAIRE, "Claire", "C", null),
                utilisateur(UUID.randomUUID(), "Dan", "D", null),
                utilisateur(UUID.randomUUID(), "Eve", "E", null)));
        when(clientRepository.findAll()).thenReturn(List.of());

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(PageRequest.of(1, 2),
                UtilisateurFilterCriteria.empty());

        assertThat(page.getContent()).extracting(UtilisateurDTO::nom).containsExactly("C", "D");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void triDemandeSurLePrenomDescendant() {
        when(identityGateway.utilisateurs()).thenReturn(List.of(
                utilisateur(ID_ALICE, "Alice", "Dupont", null),
                utilisateur(ID_BOB, "Bob", "Martin", null),
                utilisateur(ID_CLAIRE, "Claire", "Durand", null)));
        when(clientRepository.findAll()).thenReturn(List.of());

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "prenom")), UtilisateurFilterCriteria.empty());

        assertThat(page.getContent()).extracting(UtilisateurDTO::prenom)
                .containsExactly("Claire", "Bob", "Alice");
    }

    @Test
    void champDeTriInconnuEstRefuse() {
        when(identityGateway.utilisateurs()).thenReturn(List.of());
        when(clientRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> utilisateurService.getUtilisateurs(
                PageRequest.of(0, 20, Sort.by("password")), UtilisateurFilterCriteria.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    void creationNormaliseLaCommandeAvantKeycloak() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client(CLIENT_ID, "La breteche")));
        when(clientRepository.findAll()).thenReturn(List.of(client(CLIENT_ID, "La breteche")));
        when(identityGateway.creerUtilisateur(any())).thenReturn(ID_ALICE);
        when(identityGateway.utilisateur(ID_ALICE))
                .thenReturn(Optional.of(utilisateur(ID_ALICE, "Alice", "Dupont", "La breteche")));

        UtilisateurDTO cree = utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "  Alice ", "Dupont", " Alice@Exemple.fr ", List.of("ADMIN"), CLIENT_ID, null));

        ArgumentCaptor<IdentiteCommande> capteur = ArgumentCaptor.forClass(IdentiteCommande.class);
        verify(identityGateway).creerUtilisateur(capteur.capture());
        assertThat(capteur.getValue().prenom()).isEqualTo("Alice");
        assertThat(capteur.getValue().nom()).isEqualTo("Dupont");
        assertThat(capteur.getValue().email()).isEqualTo("Alice@Exemple.fr");
        assertThat(capteur.getValue().roles()).containsExactly("admin");
        assertThat(capteur.getValue().groupe()).isEqualTo("La breteche");
        assertThat(capteur.getValue().actif()).isTrue();

        assertThat(cree.id()).isEqualTo(ID_ALICE);
        assertThat(cree.clientId()).isEqualTo(CLIENT_ID);
    }

    @Test
    void creationSansClientNeRattacheAucunGroupe() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));
        when(identityGateway.creerUtilisateur(any())).thenReturn(ID_BOB);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(identityGateway.utilisateur(ID_BOB)).thenReturn(Optional.of(utilisateur(ID_BOB, "Bob", "Martin", null)));

        utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "Bob", "Martin", "bob@exemple.fr", List.of("user"), null, null));

        ArgumentCaptor<IdentiteCommande> capteur = ArgumentCaptor.forClass(IdentiteCommande.class);
        verify(identityGateway).creerUtilisateur(capteur.capture());
        assertThat(capteur.getValue().groupe()).isNull();
        verify(clientRepository, never()).findById(any());
    }

    @Test
    void creationAvecRoleInconnuEstRefuseeAvantToutAppelKeycloak() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));

        assertThatThrownBy(() -> utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of("dpo"), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dpo");

        verify(identityGateway, never()).creerUtilisateur(any());
    }

    @Test
    void creationAvecListeDeRolesVideEstRefusee() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));

        assertThatThrownBy(() -> utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of(" "), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rôle");

        verify(identityGateway, never()).creerUtilisateur(any());
    }

    @Test
    void creationAvecClientInconnuEstRefusee() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));
        when(clientRepository.findById(AUTRE_CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of("user"), AUTRE_CLIENT_ID, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client");

        verify(identityGateway, never()).creerUtilisateur(any());
    }

    @Test
    void lectureDunUtilisateurCreeMaisIllisible() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));
        when(identityGateway.creerUtilisateur(any())).thenReturn(ID_ALICE);
        when(clientRepository.findAll()).thenReturn(List.of());
        when(identityGateway.utilisateur(ID_ALICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.creerUtilisateur(new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of("admin"), null, null)))
                .isInstanceOf(IdentityProviderException.class);
    }

    @Test
    void modificationDunUtilisateurInconnuEstRefusee() {
        when(identityGateway.utilisateur(ID_ALICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.modifierUtilisateur(ID_ALICE, new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of("admin"), null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(identityGateway, never()).modifierUtilisateur(any(), any());
    }

    @Test
    void modificationDelegueEtlitLUtilisateurModifie() {
        when(identityGateway.utilisateur(ID_ALICE))
                .thenReturn(Optional.of(utilisateur(ID_ALICE, "Alice", "Dupont", null)))
                .thenReturn(Optional.of(utilisateur(ID_ALICE, "Alice", "Dupont", null)));
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));
        when(clientRepository.findAll()).thenReturn(List.of());

        utilisateurService.modifierUtilisateur(ID_ALICE, new UtilisateurWriteDTO(
                "Alice", "Dupont", "alice@exemple.fr", List.of("user"), null, false));

        ArgumentCaptor<IdentiteCommande> capteur = ArgumentCaptor.forClass(IdentiteCommande.class);
        verify(identityGateway).modifierUtilisateur(eq(ID_ALICE), capteur.capture());
        assertThat(capteur.getValue().roles()).containsExactly("user");
        assertThat(capteur.getValue().actif()).isFalse();
    }

    @Test
    void suppressionDelegueeAKeycloak() {
        utilisateurService.supprimerUtilisateur(ID_ALICE);

        verify(identityGateway).supprimerUtilisateur(ID_ALICE);
        verify(identityGateway, never()).utilisateurs();
    }

    @Test
    void utilisateurInconnuEnLectureSeule() {
        when(clientRepository.findAll()).thenReturn(List.of());
        when(identityGateway.utilisateur(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.getUtilisateur(ID_BOB))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rolesDisponiblesViennentDeKeycloak() {
        when(identityGateway.rolesDisponibles()).thenReturn(List.of("admin", "user"));

        assertThat(utilisateurService.getRoles()).containsExactly("admin", "user");
        verify(identityGateway).rolesDisponibles();
    }

    @Test
    void rechercheInsensibleAuNomVide() {
        when(identityGateway.utilisateurs()).thenReturn(List.of(utilisateur(ID_ALICE, "Alice", null, null)));
        when(clientRepository.findAll()).thenReturn(List.of());

        Page<UtilisateurDTO> page = utilisateurService.getUtilisateurs(PageRequest.of(0, 20),
                new UtilisateurFilterCriteria("du", null, null));

        assertThat(page.getContent()).isEmpty();
    }

    private static IdentiteUtilisateur utilisateur(UUID id, String prenom, String nom, String groupe) {
        return new IdentiteUtilisateur(id, "%s.%s@exemple.fr".formatted(prenom, nom), prenom, nom,
                "%s.%s@exemple.fr".formatted(prenom, nom), true, List.of("user"), groupe);
    }

    private static Client client(UUID id, String nom) {
        return Client.builder().id(id).nom(nom).statut("actif").build();
    }
}
