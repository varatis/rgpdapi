package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakClientRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakGroupRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakRoleRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakUserRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityGatewayTest {

    private static final UUID ID_ALICE = UUID.fromString("d6dfd117-8047-4a9a-afca-f5268a38bfcf");
    private static final UUID ID_BOB = UUID.fromString("6a04222b-60f8-434b-bdff-c01ce36fde2f");
    private static final UUID ID_CLAIRE = UUID.fromString("e9048a22-e73d-4b35-b08a-0540c58e7a6f");
    private static final String UUID_CLIENT = "client-uuid";
    private static final String CHEMIN_CLIENT = "/clients?clientId=minds-saas-rgpd";

    @Mock
    private KeycloakAdminClient client;

    private KeycloakProperties properties;
    private KeycloakIdentityGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new KeycloakProperties();
        properties.setRealm("minds-rgpd");
        properties.setResourceClientId("minds-saas-rgpd");
        properties.setTaillePage(100);
        properties.setNombreMaxResultats(1000);
        gateway = new KeycloakIdentityGateway(properties, client);
    }

    @Test
    void creationEnvoieLeMotDePasseTemporairePuisLieRoleEtGroupe() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        clientAvecUuid();
        doReturn(Optional.of(role("role-admin", "admin")))
                .when(client).lire(eq("/clients/" + UUID_CLIENT + "/roles/admin"), any());
        doReturn(Optional.of(List.of(groupe("group-alpha", "La breteche"))))
                .when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());

        UUID cree = gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont", "alice@exemple.fr",
                List.of("admin"), "La breteche", true));

        assertThat(cree).isEqualTo(ID_ALICE);

        ArgumentCaptor<Object> capteurCorps = ArgumentCaptor.forClass(Object.class);
        verify(client).creer(eq("/users"), capteurCorps.capture());
        Map<String, Object> corps = capturerCorps(capteurCorps.getValue());
        assertThat(corps)
                .containsEntry("username", "alice@exemple.fr")
                .containsEntry("email", "alice@exemple.fr")
                .containsEntry("firstName", "Alice")
                .containsEntry("lastName", "Dupont")
                .containsEntry("enabled", true)
                .containsEntry("emailVerified", false)
                .containsEntry("requiredActions", List.of("UPDATE_PASSWORD"));

        List<Map<String, Object>> credentials = (List<Map<String, Object>>) corps.get("credentials");
        assertThat(credentials).singleElement().satisfies(credential -> {
            assertThat(credential).containsEntry("type", "password").containsEntry("temporary", true);
            assertThat(String.valueOf(credential.get("value"))).hasSize(18);
        });

        verify(client).ajouter(eq("/users/" + ID_ALICE + "/role-mappings/clients/" + UUID_CLIENT),
                eq(List.of(role("role-admin", "admin"))));
        verify(client).modifier("/users/" + ID_ALICE + "/groups/group-alpha", null);
    }

    @Test
    void creationSansClientNeToucheAuxGroupes() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        clientAvecUuid();
        doReturn(Optional.of(role("role-user", "user")))
                .when(client).lire(eq("/clients/" + UUID_CLIENT + "/roles/user"), any());

        gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont", "alice@exemple.fr",
                List.of("user"), null, true));

        verify(client, never()).lire(startsWith("/groups"), any());
        verify(client, never()).modifier(anyString(), any());
    }

    @Test
    void conflitKeycloakDevientDoublonLocal() {
        when(client.creer(eq("/users"), any()))
                .thenThrow(new IdentityProviderException("POST", 409, "User exists with same email"));

        assertThatThrownBy(() -> gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont",
                "alice@exemple.fr", List.of("admin"), null, true)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@exemple.fr");

        verify(client, never()).supprimer(anyString());
    }

    @Test
    void echecDeLiaisonSupprimeLUtilisateurCree() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        clientAvecUuid();

        assertThatThrownBy(() -> gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont",
                "alice@exemple.fr", List.of("inconnu"), null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconnu");

        verify(client).supprimer("/users/" + ID_ALICE);
    }

    @Test
    void echecDeLiaisonPropageErreurKeycloak() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        clientAvecUuid();
        doReturn(Optional.of(role("role-admin", "admin")))
                .when(client).lire(eq("/clients/" + UUID_CLIENT + "/roles/admin"), any());
        doThrow(new IdentityProviderException("POST", 500, "keycloak en panne"))
                .when(client).ajouter(anyString(), any());

        assertThatThrownBy(() -> gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont",
                "alice@exemple.fr", List.of("admin"), null, true)))
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("keycloak en panne");

        verify(client).supprimer("/users/" + ID_ALICE);
    }

    @Test
    void supprimeSilencieusementQuandLaCompensationEchoue() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        clientAvecUuid();
        doThrow(new IdentityProviderException("LECTURE", 503, "indisponible"))
                .when(client).supprimer(eq("/users/" + ID_ALICE));

        assertThatThrownBy(() -> gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont",
                "alice@exemple.fr", List.of("inconnu"), null, true)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listePagineeSansChargerLesRoles() {
        doReturn(Optional.of(List.of(
                utilisateur(ID_ALICE.toString(), "Alice", "Dupont", "alice@exemple.fr",
                        List.of(groupe("group-alpha", "La breteche"))),
                utilisateur(ID_BOB.toString(), "Bob", "Martin", "bob@exemple.fr", List.of()))))
                .when(client).lire(eq("/users?first=0&max=2&briefRepresentation=false"), any());
        doReturn(Optional.of(List.of(
                utilisateur(ID_CLAIRE.toString(), "Claire", "Durand", "claire@exemple.fr",
                        List.of(groupe("group-beta", "Entreprise Alpha"))))))
                .when(client).lire(eq("/users?first=2&max=2&briefRepresentation=false"), any());

        properties.setTaillePage(2);
        properties.setNombreMaxResultats(10);

        List<IdentiteUtilisateur> utilisateurs = gateway.utilisateurs();

        assertThat(utilisateurs).hasSize(3);
        assertThat(utilisateurs.getFirst().nom()).isEqualTo("Dupont");
        assertThat(utilisateurs.getFirst().groupe()).isEqualTo("La breteche");
        assertThat(utilisateurs.getFirst().roles()).isEmpty();
        assertThat(utilisateurs.get(2).nom()).isEqualTo("Durand");
        assertThat(utilisateurs.get(2).groupe()).isEqualTo("Entreprise Alpha");
        verify(client, never()).lire(startsWith("/clients"), any());
    }

    @Test
    void rolesDemandesParIdentitePourLaPage() {
        clientAvecUuid();
        doReturn(Optional.of(List.of(role("r-admin", "ADMIN"), role("r-bis", "admin"))))
                .when(client).lire(eq("/users/" + ID_ALICE + "/role-mappings/clients/" + UUID_CLIENT), any());
        doReturn(Optional.of(List.of(role("r-user", "user"), role("r-admin", "ADMIN"))))
                .when(client).lire(eq("/users/" + ID_BOB + "/role-mappings/clients/" + UUID_CLIENT), any());

        Map<UUID, List<String>> roles = gateway.rolesDesUtilisateurs(Arrays.asList(ID_ALICE, ID_BOB, null));

        assertThat(roles).hasSize(2);
        assertThat(roles.get(ID_ALICE)).containsExactly("ADMIN");
        assertThat(roles.get(ID_BOB)).containsExactly("user", "ADMIN");
    }

    @Test
    void seuilDeLectureInterromptLePaging() {
        properties.setTaillePage(2);
        properties.setNombreMaxResultats(2);
        doReturn(Optional.of(List.of(
                utilisateur(ID_ALICE.toString(), "Alice", "Dupont", "alice@exemple.fr", List.of()),
                utilisateur(ID_BOB.toString(), "Bob", "Martin", "bob@exemple.fr", List.of()))))
                .when(client).lire(eq("/users?first=0&max=2&briefRepresentation=false"), any());

        assertThat(gateway.utilisateurs()).hasSize(2);
        verify(client, never()).lire(eq("/users?first=2&max=2&briefRepresentation=false"), any());
    }

    @Test
    void utilisateurIntrouvableENLecture() {
        assertThat(gateway.utilisateur(ID_ALICE)).isEmpty();
    }

    @Test
    void suppressionDunUtilisateurInconnu() {
        assertThatThrownBy(() -> gateway.supprimerUtilisateur(ID_ALICE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(client, never()).supprimer(anyString());
    }

    @Test
    void suppressionVerifiePuisSupprime() {
        doReturn(Optional.of(new HashMap<String, Object>(Map.of("id", ID_ALICE.toString()))))
                .when(client).lire(eq("/users/" + ID_ALICE), any());

        gateway.supprimerUtilisateur(ID_ALICE);

        verify(client).supprimer("/users/" + ID_ALICE);
    }

    @Test
    void membresDunGroupeInexistant() {
        assertThat(gateway.membresDuGroupe("Inconnu")).isEmpty();
    }

    @Test
    void membresDunGroupeSontLeursIdentifiants() {
        doReturn(Optional.of(List.of(groupe("group-alpha", "La breteche"))))
                .when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());
        doReturn(Optional.of(List.of(utilisateur(ID_ALICE.toString(), "Alice", "Dupont", "a@e.fr", List.of()))))
                .when(client).lire(eq("/groups/group-alpha/members?first=0&max=100&briefRepresentation=true"), any());

        assertThat(gateway.membresDuGroupe("La breteche")).containsExactly(ID_ALICE);
    }

    @Test
    void suppressionDeGroupeInexistantNeRienAppelle() {
        gateway.supprimerGroupe("La breteche");

        verify(client, never()).supprimer(anyString());
    }

    @Test
    void suppressionDeGroupeConnu() {
        doReturn(Optional.of(List.of(groupe("group-alpha", "La breteche"))))
                .when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());

        gateway.supprimerGroupe("La breteche");

        verify(client).supprimer("/groups/group-alpha");
    }

    @Test
    void creationDeGroupeDejaPresentEstIgnoree() {
        doReturn(Optional.of(List.of(groupe("group-alpha", "La breteche"))))
                .when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());

        gateway.creerGroupe("La breteche");

        verify(client, never()).creer(anyString(), any());
    }

    @Test
    void renommageDeGroupeAbsentCreeLeNouveau() {
        when(client.creer(eq("/groups"), any())).thenReturn(Optional.of("group-beta"));

        gateway.renommerGroupe("Ancien", "Nouveau");

        verify(client).creer(eq("/groups"), any());
    }

    @Test
    void renommageRefuseUnNomDejaPris() {
        doReturn(Optional.of(List.of(groupe("group-alpha", "Ancien"))))
                .when(client).lire(eq("/groups?search=Ancien&exact=true"), any());
        doReturn(Optional.of(List.of(groupe("group-beta", "Nouveau"))))
                .when(client).lire(eq("/groups?search=Nouveau&exact=true"), any());

        assertThatThrownBy(() -> gateway.renommerGroupe("Ancien", "Nouveau"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(client, never()).modifier(anyString(), any());
    }

    @Test
    void renommageDeGroupe() {
        doReturn(Optional.of(List.of(groupe("group-alpha", "Ancien"))))
                .when(client).lire(eq("/groups?search=Ancien&exact=true"), any());
        doReturn(Optional.empty()).when(client).lire(eq("/groups?search=Nouveau&exact=true"), any());

        gateway.renommerGroupe("Ancien", "Nouveau");

        ArgumentCaptor<Object> capteur = ArgumentCaptor.forClass(Object.class);
        verify(client).modifier(eq("/groups/group-alpha"), capteur.capture());
        assertThat((Map<String, Object>) capteur.getValue())
                .containsEntry("id", "group-alpha")
                .containsEntry("name", "Nouveau");
    }

    @Test
    void rolesDisponiblesConservesEtDeDoublonnes() {
        clientAvecUuid();
        doReturn(Optional.of(List.of(role("r1", "ADMIN"), role("r2", "user"), role("r3", "Admin"))))
                .when(client).lire(eq("/clients/" + UUID_CLIENT + "/roles?briefRepresentation=true"), any());

        assertThat(gateway.rolesDisponibles()).containsExactly("ADMIN", "user");
    }

    @Test
    void clientResourceAbsentDuRealm() {
        assertThatThrownBy(() -> gateway.rolesDisponibles())
                .isInstanceOf(IdentityProviderException.class)
                .hasMessageContaining("minds-saas-rgpd");
    }

    @Test
    void modificationConserveLesChampsNonGeressEtSynchroniseLesRoles() {
        Map<String, Object> brut = new HashMap<>();
        brut.put("id", ID_ALICE.toString());
        brut.put("username", "ancien@exemple.fr");
        brut.put("attributes", Map.of("matricule", "12345"));
        doReturn(Optional.of(brut)).when(client).lire(eq("/users/" + ID_ALICE), any());

        clientAvecUuid();
        doReturn(Optional.of(List.of(role("r-user", "user"))))
                .when(client).lire(eq("/users/" + ID_ALICE + "/role-mappings/clients/" + UUID_CLIENT), any());
        doReturn(Optional.of(role("r-admin", "admin")))
                .when(client).lire(eq("/clients/" + UUID_CLIENT + "/roles/admin"), any());

        gateway.modifierUtilisateur(ID_ALICE, new IdentiteCommande("Alice", "Dupont", "nouvel@exemple.fr",
                List.of("admin"), null, false));

        ArgumentCaptor<Object> capteur = ArgumentCaptor.forClass(Object.class);
        verify(client).modifier(eq("/users/" + ID_ALICE), capteur.capture());
        assertThat((Map<String, Object>) capteur.getValue())
                .containsEntry("firstName", "Alice")
                .containsEntry("lastName", "Dupont")
                .containsEntry("email", "nouvel@exemple.fr")
                .containsEntry("username", "nouvel@exemple.fr")
                .containsEntry("enabled", false)
                .containsEntry("attributes", Map.of("matricule", "12345"));

        String cheminRoles = "/users/" + ID_ALICE + "/role-mappings/clients/" + UUID_CLIENT;
        verify(client).ajouter(eq(cheminRoles), eq(List.of(role("r-admin", "admin"))));
        verify(client).supprimer(eq(cheminRoles), eq(List.of(role("r-user", "user"))));
    }

    @Test
    void modificationDunUtilisateurInconnu() {
        assertThatThrownBy(() -> gateway.modifierUtilisateur(ID_ALICE,
                new IdentiteCommande("Alice", "Dupont", "a@e.fr", List.of("admin"), null, true)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(client, never()).modifier(anyString(), any());
    }

    @Test
    void changementDeClientQuitteAncienGroupe() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        doReturn(Optional.of(List.of(groupe("group-beta", "Entreprise Alpha"))))
                .when(client).lire(eq("/groups?search=Entreprise%20Alpha&exact=true"), any());
        doReturn(Optional.of(utilisateur(ID_ALICE.toString(), "Alice", "Dupont", "a@e.fr",
                List.of(groupe("group-alpha", "La breteche")))))
                .when(client).lire(eq("/users/" + ID_ALICE), any());

        gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont", "a@e.fr",
                List.of(), "Entreprise Alpha", true));

        verify(client).supprimer("/users/" + ID_ALICE + "/groups/group-alpha");
        verify(client).modifier("/users/" + ID_ALICE + "/groups/group-beta", null);
    }

    @Test
    void groupeRattacheCreeSiAbsent() {
        when(client.creer(eq("/users"), any())).thenReturn(Optional.of(ID_ALICE.toString()));
        when(client.creer(eq("/groups"), any())).thenReturn(Optional.of("group-alpha"));
        doReturn(Optional.empty()).when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());

        gateway.creerUtilisateur(new IdentiteCommande("Alice", "Dupont", "a@e.fr",
                List.of(), "La breteche", true));

        verify(client).creer(eq("/groups"), any());
        verify(client).modifier("/users/" + ID_ALICE + "/groups/group-alpha", null);
    }

    @Test
    void encodageDesNomsDeGroupes() {
        doReturn(Optional.of(List.of(groupe("g1", "La breteche"))))
                .when(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());

        assertThat(gateway.membresDuGroupe("La breteche")).isEmpty();
        verify(client).lire(eq("/groups?search=La%20breteche&exact=true"), any());
    }

    @Test
    void identifiantConvertiEnUuid() {
        assertThat(KeycloakIdentityGateway.versUuid(ID_ALICE.toString())).isEqualTo(ID_ALICE);
        assertThat(KeycloakIdentityGateway.versUuid("pas-un-uuid")).isNull();
        assertThat(KeycloakIdentityGateway.versUuid(null)).isNull();
    }

    private void clientAvecUuid() {
        doReturn(Optional.of(List.of(new KeycloakClientRepresentation(UUID_CLIENT, "minds-saas-rgpd", true, false))))
                .when(client).lire(eq(CHEMIN_CLIENT), any());
    }

    private static Map<String, Object> capturerCorps(Object corps) {
        return (Map<String, Object>) corps;
    }

    private static KeycloakUserRepresentation utilisateur(String id, String prenom, String nom, String email,
                                                           List<KeycloakGroupRepresentation> groupes) {
        return new KeycloakUserRepresentation(id, email, prenom, nom, email, true, groupes);
    }

    private static KeycloakGroupRepresentation groupe(String id, String nom) {
        return new KeycloakGroupRepresentation(id, nom, "/" + nom);
    }

    private static KeycloakRoleRepresentation role(String id, String nom) {
        return new KeycloakRoleRepresentation(id, nom, null, false, true, UUID_CLIENT);
    }
}
