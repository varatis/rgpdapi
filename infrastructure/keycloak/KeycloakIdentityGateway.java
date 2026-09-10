package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.identity.GroupeIdentite;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implémentation Keycloak de la passerelle d'identité : les utilisateurs,
 * leurs rôles applicatifs (rôles client) et leur rattachement à un client
 * (un groupe par client sous le préfixe configuré) vivent dans Keycloak.
 *
 * <p>Les listes d'utilisateurs renvoyées par Keycloak ne portent ni rôles ni
 * groupes : ceux-ci sont reconstitués par des requêtes inverses — une par rôle
 * et une par groupe de client — plutôt qu'une paire de requêtes par
 * utilisateur.</p>
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
        Map<UUID, List<String>> rolesParUtilisateur = rolesParUtilisateur();
        Map<UUID, String> groupeParUtilisateur = groupeParUtilisateur();
        List<KeycloakUserRepresentation> users = adminClient.getUsers();
        List<IdentiteUtilisateur> result = new ArrayList<>(users.size());
        for (KeycloakUserRepresentation user : users) {
            result.add(toIdentite(user,
                    rolesParUtilisateur.getOrDefault(user.getId(), List.of()),
                    groupeParUtilisateur.get(user.getId())));
        }
        return result;
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return adminClient.getUserById(id).map(user -> {
            String clientUuid = clientUuid();
            List<String> roles = clientUuid == null
                    ? List.of()
                    : adminClient.getUserClientRoles(id, clientUuid).stream()
                            .map(KeycloakRoleRepresentation::getName)
                            .collect(Collectors.toList());
            return toIdentite(user, roles, groupePrincipal(adminClient.getUserGroups(id)));
        });
    }

    @Override
    public Optional<IdentiteUtilisateur> parEmail(String email) {
        return adminClient.getUserByEmail(email)
                .flatMap(user -> utilisateur(user.getId()));
    }

    /**
     * Crée l'utilisateur, l'affecte à son groupe de client puis lui attribue
     * ses rôles : sans cette dernière étape, le compte serait inutilisable
     * côté autorisations malgré un POST affichant des rôles.
     */
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
            affecterAuGroupe(userId, commande.groupe());
        }

        if (commande.roles() != null && !commande.roles().isEmpty()) {
            adminClient.assignClientRoles(userId, clientUuidObligatoire(), commande.roles());
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
            retirerDesAutresGroupesClients(id, commande.groupe());
            affecterAuGroupe(id, commande.groupe());
        }

        if (commande.roles() != null && !commande.roles().isEmpty()) {
            String clientUuid = clientUuidObligatoire();
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
        return List.copyOf(nomsRolesClient());
    }

    @Override
    public void affecterRole(UUID id, String role) {
        adminClient.assignClientRoles(id, clientUuidObligatoire(), List.of(role));
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

    /**
     * Affecte l'utilisateur à son groupe de client, en créant le groupe s'il
     * n'existe pas encore : sans lui, l'utilisateur ne serait rattaché à
     * aucun client.
     */
    private void affecterAuGroupe(UUID userId, String nomGroupe) {
        GroupeIdentite groupe = groupe(nomGroupe).orElseGet(() -> creerGroupe(nomGroupe));
        adminClient.addUserToGroup(userId, groupe.id());
    }

    /**
     * Retire l'utilisateur de ses groupes de clients autres que celui visé :
     * un utilisateur appartient à un seul client à la fois.
     */
    private void retirerDesAutresGroupesClients(UUID userId, String nomGroupeConserve) {
        for (KeycloakGroupRepresentation groupe : adminClient.getUserGroups(userId)) {
            if (groupe.getPath() != null
                    && groupe.getPath().startsWith(properties.getGroupPrefix())
                    && !groupe.getName().equals(nomGroupeConserve)) {
                adminClient.removeUserFromGroup(userId, groupe.getId());
            }
        }
    }

    private IdentiteUtilisateur toIdentite(KeycloakUserRepresentation user, List<String> roles, String groupe) {
        return new IdentiteUtilisateur(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.isEnabled(),
                roles,
                groupe
        );
    }

    /**
     * Rôles client par utilisateur : une requête par rôle (les titulaires
     * d'un rôle), au lieu d'une requête par utilisateur.
     */
    private Map<UUID, List<String>> rolesParUtilisateur() {
        Map<UUID, List<String>> roles = new HashMap<>();
        String clientUuid = clientUuid();
        if (clientUuid == null) {
            return roles;
        }
        for (KeycloakRoleRepresentation role : adminClient.getClientRoles(clientUuid)) {
            for (KeycloakUserRepresentation titulaire : adminClient.getUsersByClientRole(clientUuid, role.getName())) {
                roles.computeIfAbsent(titulaire.getId(), id -> new ArrayList<>()).add(role.getName());
            }
        }
        return roles;
    }

    /**
     * Groupe de client par utilisateur : parcours des sous-groupes du groupe
     * parent configuré (ex. {@code /clients}) puis de leurs membres. Le
     * premier groupe trouvé l'emporte si les données étaient incohérentes.
     */
    private Map<UUID, String> groupeParUtilisateur() {
        Map<UUID, String> groupes = new HashMap<>();
        groupe(nomGroupeParent()).ifPresent(parent ->
                adminClient.getGroupChildren(parent.id()).forEach(groupe ->
                        adminClient.getGroupMembers(groupe.getId()).forEach(membre ->
                                groupes.putIfAbsent(membre.getId(), groupe.getName()))));
        return groupes;
    }

    private String groupePrincipal(List<KeycloakGroupRepresentation> groupes) {
        for (KeycloakGroupRepresentation groupe : groupes) {
            if (groupe.getPath() != null && groupe.getPath().startsWith(properties.getGroupPrefix())) {
                return groupe.getName();
            }
        }
        return null;
    }

    /**
     * Nom du groupe parent des groupes de clients : le préfixe configuré
     * ({@code keycloak.group-prefix}, ex. {@code /clients}) désigne un
     * chemin ; le groupe racine correspondant porte le même nom sans le
     * slash initial.
     */
    private String nomGroupeParent() {
        String prefix = properties.getGroupPrefix();
        return prefix.startsWith("/") ? prefix.substring(1) : prefix;
    }

    /**
     * L'API de rôles client de Keycloak est adressée par l'UUID interne du
     * client, pas par son identifiant public ({@code resourceClientId}).
     */
    private String clientUuidObligatoire() {
        String uuid = clientUuid();
        if (uuid == null) {
            throw new IdentityProviderException("client Keycloak introuvable",
                    "resourceClientId", properties.getResourceClientId());
        }
        return uuid;
    }

    private String clientUuid() {
        return adminClient.getClientUuidByResourceId(properties.getResourceClientId());
    }

    private Set<String> nomsRolesClient() {
        String clientUuid = clientUuid();
        if (clientUuid == null) {
            return Set.of();
        }
        return adminClient.getClientRoles(clientUuid).stream()
                .map(KeycloakRoleRepresentation::getName)
                .collect(Collectors.toSet());
    }
}
