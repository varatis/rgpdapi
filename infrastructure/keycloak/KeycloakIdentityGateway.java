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
    public void definirMotDePasse(UUID id, String motDePasse) {
        adminClient.resetPassword(id, motDePasse, true);
    }

    @Override
    public List<String> rolesDisponibles() {
        return List.copyOf(nomsRolesClient());
    }

    @Override
    public void affecterRole(UUID id, String role) {
        adminClient.assignClientRoles(id, clientUuidObligatoire(), List.of(role));
    }

    /**
     * Groupe de client par nom : le sous-groupe DIRECT du parent configuré
     * portant ce nom, résolu dans la hiérarchie des groupes — sans passer
     * par la recherche Keycloak, dont l'encodage des espaces et le périmètre
     * (groupes racine uniquement sur certaines versions) varient selon les
     * versions. Les noms étant uniques entre frères, la correspondance par
     * nom est non ambiguë et le parent n'est jamais désignable.
     */
    @Override
    public Optional<GroupeIdentite> groupe(String nom) {
        return groupesClients().stream()
                .filter(groupe -> nom.equals(groupe.getName()))
                .findFirst()
                .map(groupe -> new GroupeIdentite(groupe.getId(), groupe.getName(), groupe.getPath()));
    }

    @Override
    public GroupeIdentite creerGroupe(String nom) {
        UUID parentId = groupeParent()
                .map(GroupeIdentite::id)
                .orElseThrow(() -> new IdentityProviderException("groupe parent des clients introuvable",
                        "chemin attendu", properties.getGroupPrefix()));
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
     * un utilisateur appartient à un seul client à la fois. La cible est
     * identifiée par son chemin (préfixe + nom) : un homonyme du groupe visé —
     * ou le groupe parent lui-même — est systématiquement retiré.
     */
    private void retirerDesAutresGroupesClients(UUID userId, String nomGroupeConserve) {
        String cheminConserve = properties.getGroupPrefix() + "/" + nomGroupeConserve;
        for (KeycloakGroupRepresentation groupe : adminClient.getUserGroups(userId)) {
            if (groupe.getPath() != null
                    && groupe.getPath().startsWith(properties.getGroupPrefix())
                    && !groupe.getPath().equals(cheminConserve)) {
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
     * Groupe de client par utilisateur : parcours des groupes de clients
     * (hiérarchie) puis des membres de chacun. Le premier groupe trouvé
     * l'emporte si les données étaient incohérentes.
     */
    private Map<UUID, String> groupeParUtilisateur() {
        Map<UUID, String> groupes = new HashMap<>();
        groupesClients().forEach(groupe ->
                adminClient.getGroupMembers(groupe.getId()).forEach(membre ->
                        groupes.putIfAbsent(membre.getId(), groupe.getName())));
        return groupes;
    }

    /**
     * Groupes de clients : sous-groupes DIRECTS du parent configuré. La
     * hiérarchie {@code GET /groups} les imbrique sur les Keycloak anciens ;
     * les versions récentes ne les imbriquent plus — d'où le repli sur
     * {@code GET /groups/{id}/children}, inexistant sur les anciennes (405
     * toléré, résultat vide).
     */
    private List<KeycloakGroupRepresentation> groupesClients() {
        return groupeParentBrut()
                .map(this::sousGroupesDu)
                .orElse(List.of());
    }

    private List<KeycloakGroupRepresentation> sousGroupesDu(KeycloakGroupRepresentation parent) {
        if (parent.getSubGroups() != null && !parent.getSubGroups().isEmpty()) {
            return parent.getSubGroups();
        }
        return adminClient.getGroupChildren(parent.getId());
    }

    /**
     * Groupe de client affiché pour un utilisateur : le premier trouvé sous
     * le préfixe, hors groupe parent — celui-ci n'est pas un client.
     */
    private String groupePrincipal(List<KeycloakGroupRepresentation> groupes) {
        for (KeycloakGroupRepresentation groupe : groupes) {
            if (groupe.getPath() != null
                    && groupe.getPath().startsWith(properties.getGroupPrefix())
                    && !groupe.getPath().equals(properties.getGroupPrefix())) {
                return groupe.getName();
            }
        }
        return null;
    }

    /**
     * Groupe parent des clients : le groupe racine dont le chemin vaut le
     * préfixe configuré (ex. {@code /clients}), lu dans la hiérarchie
     * {@code GET /groups}.
     */
    private Optional<KeycloakGroupRepresentation> groupeParentBrut() {
        return adminClient.getGroupHierarchy().stream()
                .filter(groupe -> properties.getGroupPrefix().equals(groupe.getPath()))
                .findFirst();
    }

    private Optional<GroupeIdentite> groupeParent() {
        return groupeParentBrut()
                .map(groupe -> new GroupeIdentite(groupe.getId(), groupe.getName(), groupe.getPath()));
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
        String clientUuid = clientUuidObligatoire();
        return adminClient.getClientRoles(clientUuid).stream()
                .map(KeycloakRoleRepresentation::getName)
                .collect(Collectors.toSet());
    }
}
