package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.identity.GroupeIdentite;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation Keycloak de la passerelle d'identité : les utilisateurs,
 * leurs rôles et leurs groupes (un groupe par client) vivent dans Keycloak.
 */
@Component
public class KeycloakIdentityGateway implements IdentityGateway {

    private final KeycloakAdminClient adminClient;

    private final KeycloakProperties properties;

    public KeycloakIdentityGateway(KeycloakAdminClient adminClient, KeycloakProperties properties) {
        this.adminClient = adminClient;
        this.properties = properties;
    }

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        String clientUuid = clientUuid();
        Set<String> nomsRolesClient = nomsRolesClient(clientUuid);
        List<KeycloakUserRepresentation> users = adminClient.getUsers();
        List<IdentiteUtilisateur> result = new ArrayList<>(users.size());
        for (KeycloakUserRepresentation user : users) {
            result.add(toIdentite(user, nomsRolesClient));
        }
        return result;
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return adminClient.getUserById(id)
                .map(user -> toIdentite(user, nomsRolesClient(clientUuid())));
    }

    @Override
    public Optional<IdentiteUtilisateur> parEmail(String email) {
        return adminClient.getUserByEmail(email)
                .flatMap(user -> utilisateur(user.getId()));
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", commande.email());
        representation.put("email", commande.email());
        representation.put("firstName", commande.prenom());
        representation.put("lastName", commande.nom());
        representation.put("enabled", commande.actif());
        representation.put("emailVerified", false);

        UUID userId = adminClient.createUser(representation);

        if (commande.groupe() != null && !commande.groupe().isEmpty()) {
            groupe(commande.groupe())
                    .ifPresent(groupe -> adminClient.addUserToGroup(userId, groupe.id()));
        }

        return userId;
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        Optional<IdentiteUtilisateur> existant = utilisateur(id);
        if (existant.isEmpty()) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + id);
        }

        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("firstName", commande.prenom());
        representation.put("lastName", commande.nom());
        representation.put("email", commande.email());
        representation.put("enabled", commande.actif());
        adminClient.updateUser(id, representation);

        if (commande.groupe() != null && !commande.groupe().isEmpty()) {
            groupe(commande.groupe())
                    .ifPresent(groupe -> adminClient.addUserToGroup(id, groupe.id()));
        }

        if (commande.roles() != null && !commande.roles().isEmpty()) {
            String clientUuid = clientUuid();
            adminClient.removeClientRoles(id, clientUuid, existant.get().roles());
            adminClient.assignClientRoles(id, clientUuid, commande.roles());
        }
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        adminClient.deleteUser(id);
    }

    @Override
    public List<String> rolesDisponibles() {
        return List.copyOf(nomsRolesClient(clientUuid()));
    }

    @Override
    public void affecterRole(UUID id, String role) {
        adminClient.assignClientRoles(id, clientUuid(), List.of(role));
    }

    @Override
    public Optional<GroupeIdentite> groupe(String nom) {
        return adminClient.getGroupByName(nom)
                .map(groupe -> new GroupeIdentite(groupe.getId(), groupe.getName(), groupe.getPath()));
    }

    @Override
    public GroupeIdentite creerGroupe(String nom) {
        UUID parentId = groupe(nomGroupeParent())
                .map(GroupeIdentite::id)
                .orElse(null);
        adminClient.createGroup(nom, parentId);
        return groupe(nom)
                .orElseThrow(() -> new IdentityProviderException("création du groupe", "nom", nom));
    }

    @Override
    public void supprimerGroupe(String nom) {
        groupe(nom).ifPresent(groupe -> adminClient.deleteGroup(groupe.id()));
    }

    @Override
    public List<UUID> membresGroupe(String nom) {
        return groupe(nom)
                .map(groupe -> adminClient.getGroupMembers(groupe.id()).stream()
                        .map(KeycloakUserRepresentation::getId)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public void supprimerUtilisateursDeGroupe(String nom) {
        for (UUID membreId : membresGroupe(nom)) {
            adminClient.deleteUser(membreId);
        }
    }

    private IdentiteUtilisateur toIdentite(KeycloakUserRepresentation user, Set<String> nomsRolesClient) {
        List<String> roles = user.getRealmRoles() == null
                ? List.of()
                : user.getRealmRoles().stream()
                        .filter(nomsRolesClient::contains)
                        .collect(Collectors.toList());
        return new IdentiteUtilisateur(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEnabled(),
                roles,
                groupePrincipal(user)
        );
    }

    /**
     * Nom du groupe parent des groupes de clients : le préfixe configuré
     * ({@code application.keycloak.group-prefix}, ex. {@code /clients}) désigne
     * un chemin ; le groupe racine correspondant porte le même nom sans le
     * slash initial.
     */
    private String nomGroupeParent() {
        String prefix = properties.getGroupPrefix();
        return prefix.startsWith("/") ? prefix.substring(1) : prefix;
    }

    private String groupePrincipal(KeycloakUserRepresentation user) {
        if (user.getGroups() == null) {
            return null;
        }
        for (KeycloakGroupRepresentation groupe : user.getGroups()) {
            if (groupe.getPath() != null && groupe.getPath().startsWith(properties.getGroupPrefix())) {
                return groupe.getName();
            }
        }
        return null;
    }

    /**
     * L'API de rôles client de Keycloak est adressée par l'UUID interne du
     * client, pas par son identifiant public ({@code resourceClientId}).
     */
    private String clientUuid() {
        return adminClient.getClientUuidByResourceId(properties.getResourceClientId());
    }

    private Set<String> nomsRolesClient(String clientUuid) {
        if (clientUuid == null) {
            return Set.of();
        }
        return adminClient.getClientRoles(clientUuid).stream()
                .map(KeycloakRoleRepresentation::getName)
                .collect(Collectors.toSet());
    }
}
