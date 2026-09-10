package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.identity.GroupeIdentite;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la passerelle d'identité Keycloak : le client d'administration est
 * simulé, on vérifie la reconstitution des rôles et groupes par requêtes
 * inverses et les changements de groupe / rôles à la modification.
 */
@ExtendWith(MockitoExtension.class)
class KeycloakIdentityGatewayTest {

    private static final UUID ALICE_ID = UUID.fromString("d6dfd117-8047-4a9a-afca-f5268a38bfcf");
    private static final UUID PARENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GROUPE_BRETECHE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GROUPE_ALPHA_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String CLIENT_UUID = "uuid-interne-du-client";

    @Mock
    private KeycloakAdminClient adminClient;

    @Mock
    private KeycloakProperties properties;

    @InjectMocks
    private KeycloakIdentityGateway gateway;

    private KeycloakUserRepresentation utilisateur(UUID id, String identifiant) {
        KeycloakUserRepresentation user = new KeycloakUserRepresentation();
        user.setId(id);
        user.setUsername(identifiant);
        user.setEmail(identifiant);
        user.setEnabled(true);
        return user;
    }

    private KeycloakGroupRepresentation groupe(UUID id, String nom, String chemin) {
        KeycloakGroupRepresentation groupe = new KeycloakGroupRepresentation();
        groupe.setId(id);
        groupe.setName(nom);
        groupe.setPath(chemin);
        return groupe;
    }

    private KeycloakRoleRepresentation role(String id, String nom) {
        KeycloakRoleRepresentation role = new KeycloakRoleRepresentation();
        role.setId(id);
        role.setName(nom);
        return role;
    }

    /**
     * Le listing reconstitue rôles (une requête par rôle) et groupe de client
     * (parcours des sous-groupes du parent) sans requête par utilisateur.
     */
    @Test
    void utilisateursReconstituentRolesEtGroupeParRequetesInverse() {
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);
        when(adminClient.getClientRoles(CLIENT_UUID))
                .thenReturn(List.of(role("role-admin", "admin"), role("role-user", "user")));
        when(adminClient.getUsersByClientRole(CLIENT_UUID, "admin"))
                .thenReturn(List.of(utilisateur(ALICE_ID, "alice@alpha.com")));
        when(adminClient.getUsersByClientRole(CLIENT_UUID, "user"))
                .thenReturn(List.of(utilisateur(ALICE_ID, "alice@alpha.com")));
        when(adminClient.getGroupByName("clients"))
                .thenReturn(Optional.of(groupe(PARENT_ID, "clients", "/clients")));
        when(adminClient.getGroupChildren(PARENT_ID))
                .thenReturn(List.of(groupe(GROUPE_BRETECHE_ID, "La breteche", "/clients/La breteche")));
        when(adminClient.getGroupMembers(GROUPE_BRETECHE_ID))
                .thenReturn(List.of(utilisateur(ALICE_ID, "alice@alpha.com")));
        when(adminClient.getUsers())
                .thenReturn(List.of(utilisateur(ALICE_ID, "alice@alpha.com")));

        List<IdentiteUtilisateur> utilisateurs = gateway.utilisateurs();

        assertThat(utilisateurs).hasSize(1);
        IdentiteUtilisateur alice = utilisateurs.getFirst();
        assertThat(alice.id()).isEqualTo(ALICE_ID);
        assertThat(alice.identifiant()).isEqualTo("alice@alpha.com");
        assertThat(alice.actif()).isTrue();
        assertThat(alice.roles()).containsExactly("admin", "user");
        assertThat(alice.groupe()).isEqualTo("La breteche");
    }

    /** Un utilisateur sans rôle ni groupe reste listé, avec des valeurs neutres. */
    @Test
    void utilisateurSansRoleNiGroupeResteListe() {
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);
        when(adminClient.getClientRoles(CLIENT_UUID)).thenReturn(List.of());
        when(adminClient.getGroupByName("clients")).thenReturn(Optional.empty());
        when(adminClient.getUsers())
                .thenReturn(List.of(utilisateur(ALICE_ID, "alice@alpha.com")));

        List<IdentiteUtilisateur> utilisateurs = gateway.utilisateurs();

        assertThat(utilisateurs).hasSize(1);
        assertThat(utilisateurs.getFirst().roles()).isEmpty();
        assertThat(utilisateurs.getFirst().groupe()).isNull();
    }

    /**
     * À la modification : retrait de l'ancien groupe de client, affectation au
     * nouveau, et remplacement des rôles (retrait des anciens puis ajout).
     */
    @Test
    void modifierUtilisateurChangeDeGroupeEtRemplaceLesRoles() {
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);
        when(adminClient.getUserById(ALICE_ID))
                .thenReturn(Optional.of(utilisateur(ALICE_ID, "alice@alpha.com")));
        when(adminClient.getUserClientRoles(ALICE_ID, CLIENT_UUID))
                .thenReturn(List.of(role("role-admin", "admin")));
        when(adminClient.getUserGroups(ALICE_ID))
                .thenReturn(List.of(groupe(GROUPE_BRETECHE_ID, "La breteche", "/clients/La breteche")));
        when(adminClient.getGroupByName("Entreprise Alpha"))
                .thenReturn(Optional.of(groupe(GROUPE_ALPHA_ID, "Entreprise Alpha", "/clients/Entreprise Alpha")));

        gateway.modifierUtilisateur(ALICE_ID, new IdentiteCommande(
                "Alice", "Dupont", "alice@alpha.com", List.of("user"), "Entreprise Alpha", true));

        verify(adminClient).updateUser(ALICE_ID, Map.of(
                "firstName", "Alice",
                "lastName", "Dupont",
                "email", "alice@alpha.com",
                "enabled", true));
        verify(adminClient).removeUserFromGroup(ALICE_ID, GROUPE_BRETECHE_ID);
        verify(adminClient).addUserToGroup(ALICE_ID, GROUPE_ALPHA_ID);
        verify(adminClient).removeClientRoles(ALICE_ID, CLIENT_UUID, List.of("admin"));
        verify(adminClient).assignClientRoles(ALICE_ID, CLIENT_UUID, List.of("user"));
    }

    /** Le groupe de client visé à la création est créé s'il n'existe pas encore. */
    @Test
    void creerUtilisateurCreeLeGroupeManquant() {
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(adminClient.createUser(any())).thenReturn(ALICE_ID);
        when(adminClient.getGroupByName("Nouveau Client"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(groupe(GROUPE_ALPHA_ID, "Nouveau Client", "/clients/Nouveau Client")));
        when(adminClient.getGroupByName("clients"))
                .thenReturn(Optional.of(groupe(PARENT_ID, "clients", "/clients")));
        when(adminClient.createGroup("Nouveau Client", PARENT_ID)).thenReturn(UUID.randomUUID());
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);

        UUID id = gateway.creerUtilisateur(new IdentiteCommande(
                "Alice", "Dupont", "alice@alpha.com", List.of("user"), "Nouveau Client", true));

        assertThat(id).isEqualTo(ALICE_ID);
        verify(adminClient).createGroup("Nouveau Client", PARENT_ID);
        verify(adminClient).addUserToGroup(ALICE_ID, GROUPE_ALPHA_ID);
        verify(adminClient).assignClientRoles(ALICE_ID, CLIENT_UUID, List.of("user"));
    }

    /**
     * La création attribue les rôles : sans eux, le compte serait inutilisable
     * côté autorisations malgré un POST affichant des rôles.
     */
    @Test
    void creerUtilisateurAffecteLesRoles() {
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(adminClient.createUser(any())).thenReturn(ALICE_ID);
        when(adminClient.getGroupByName("Entreprise Alpha"))
                .thenReturn(Optional.of(groupe(GROUPE_ALPHA_ID, "Entreprise Alpha", "/clients/Entreprise Alpha")));
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);

        UUID id = gateway.creerUtilisateur(new IdentiteCommande(
                "Alice", "Dupont", "alice@alpha.com", List.of("admin", "user"), "Entreprise Alpha", true));

        assertThat(id).isEqualTo(ALICE_ID);
        verify(adminClient).addUserToGroup(ALICE_ID, GROUPE_ALPHA_ID);
        verify(adminClient).assignClientRoles(ALICE_ID, CLIENT_UUID, List.of("admin", "user"));
    }

    /** Le détail d'un utilisateur interroge les endpoints dédiés rôles et groupes. */
    @Test
    void utilisateurParIdLitRolesEtGroupesDedies() {
        when(properties.getResourceClientId()).thenReturn("minds-saas-rgpd");
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(adminClient.getClientUuidByResourceId("minds-saas-rgpd")).thenReturn(CLIENT_UUID);
        when(adminClient.getUserById(ALICE_ID))
                .thenReturn(Optional.of(utilisateur(ALICE_ID, "alice@alpha.com")));
        when(adminClient.getUserClientRoles(ALICE_ID, CLIENT_UUID))
                .thenReturn(List.of(role("role-user", "user")));
        when(adminClient.getUserGroups(ALICE_ID))
                .thenReturn(List.of(groupe(GROUPE_BRETECHE_ID, "La breteche", "/clients/La breteche")));

        Optional<IdentiteUtilisateur> alice = gateway.utilisateur(ALICE_ID);

        assertThat(alice).isPresent();
        assertThat(alice.get().roles()).containsExactly("user");
        assertThat(alice.get().groupe()).isEqualTo("La breteche");
    }

    /** creerGroupe crée sous le parent configuré et reconstitue le groupe créé. */
    @Test
    void creerGroupeCreeSousLeParentConfigure() {
        when(properties.getGroupPrefix()).thenReturn("/clients");
        when(adminClient.getGroupByName("clients"))
                .thenReturn(Optional.of(groupe(PARENT_ID, "clients", "/clients")));
        when(adminClient.createGroup("Entreprise Alpha", PARENT_ID)).thenReturn(UUID.randomUUID());
        when(adminClient.getGroupByName("Entreprise Alpha"))
                .thenReturn(Optional.of(groupe(GROUPE_ALPHA_ID, "Entreprise Alpha", "/clients/Entreprise Alpha")));

        GroupeIdentite cree = gateway.creerGroupe("Entreprise Alpha");

        assertThat(cree.id()).isEqualTo(GROUPE_ALPHA_ID);
        assertThat(cree.nom()).isEqualTo("Entreprise Alpha");
        assertThat(cree.chemin()).isEqualTo("/clients/Entreprise Alpha");
    }
}
