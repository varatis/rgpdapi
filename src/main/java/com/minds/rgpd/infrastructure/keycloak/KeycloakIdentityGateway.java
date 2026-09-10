package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.GroupeIdentite;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stream;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

public class KeycloakIdentityGateway implements IdentityGateway {

    private final KeycloakAdminClient adminClient;

    private final KeycloakProperties properties;

    public KeycloakIdentityGateway(KeycloakAdminClient adminClient, KeycloakProperties properties) {
        this.adminClient = adminClient;
        this.properties = properties;
    }

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        List<KeycloakUserRepresentation> keycloakUsers = adminClient.getGroupMembers(null);
        List<IdentiteUtilisateur> result = new ArrayList<>();
        for (KeycloakUserRepresentation user : keycloakUsers) {
            List<String> roles = adminClient.getClientRoles(properties.getResourceClientId()).stream()
                    .filter(r -> user.getRealmRoles() != null && user.getRealmRoles().contains(r.getName()))
                    .map(KeycloakRoleRepresentation::getName)
                    .collect(Collectors.toList());
            String groupe = null;
            if (user.getGroups() != null) {
                for (KeycloakGroupRepresentation g : user.getGroups()) {
                    if (g.getPath() != null && g.getPath().startsWith(properties.getGroupPrefix())) {
                        groupe = g.getName();
                        break;
                    }
                }
            }
            result.add(new IdentiteUtilisateur(
                    user.getId(),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.isEnabled(),
                    roles,
                    groupe
            ));
        }
        return result;
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        Optional<KeycloakUserRepresentation> user = adminClient.getUserById(id);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        List<String> roles = adminClient.getClientRoles(properties.getResourceClientId()).stream()
                .map(KeycloakRoleRepresentation::getName)
                .collect(Collectors.toList());
        String groupe = null;
        if (user.get().getGroups() != null) {
            for (KeycloakGroupRepresentation g : user.get().getGroups()) {
                if (g.getPath() != null && g.getPath().startsWith(properties.getGroupPrefix())) {
                    groupe = g.getName();
                    break;
                }
            }
        }
        return Optional.of(new IdentiteUtilisateur(
                user.get().getId(),
                user.get().getUsername(),
                user.get().getFirstName(),
                user.get().getLastName(),
                user.get().getEmail(),
                user.get().isEnabled(),
                roles,
                groupe
        ));
    }

    @Override
    public Optional<IdentiteUtilisateur> parEmail(String email) {
        Optional<KeycloakUserRepresentation> user = adminClient.getUserByEmail(email);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        return utilisateur(user.get().getId()).map(u -> u);
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("username", commande.email());
        representation.put("email", commande.email());
        representation.put("firstName", commande.prenom());
        representation.put("lastName", commande.nom());
        representation.put("enabled", true);
        representation.put("emailVerified", false);

        if (commande.roles() != null && !commande.roles().isEmpty()) {
            representation.put("roles", commande.roles());
        }

        UUID userId = adminClient.createUser(representation);

        if (commande.groupe() != null && !commande.groupe().isEmpty()) {
            UUID groupId = findGroupByName(commande.groupe()).orElse(null);
            if (groupId != null) {
                adminClient.addUserToGroup(userId, groupId);
            }
        }

        return userId;
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        Optional<IdentiteUtilisateur> existing = utilisateur(id);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Utilisateur introuvable: " + id);
        }

        Map<String, Object> representation = new LinkedHashMap<>();
        representation.put("id", id.toString());
        representation.put("firstName", commande.prenom());
        representation.put("lastName", commande.nom());
        representation.put("email", commande.email());
        representation.put("enabled", commande.actif());

        adminClient.updateUser(id, representation);

        if (commande.groupe() != null && !commande.groupe().isEmpty()) {
            UUID groupId = findGroupByName(commande.groupe()).orElse(null);
            if (groupId != null) {
                adminClient.addUserToGroup(id, groupId);
            }
        }

        if (commande.roles() != null && !commande.roles().isEmpty()) {
            adminClient.removeClientRoles(id, properties.getResourceClientId(), existing.get().roles());
            adminClient.assignClientRoles(id, properties.getResourceClientId(), commande.roles());
        }
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        adminClient.deleteUser(id);
    }

    @Override
    public List<String> rolesDisponibles() {
        return adminClient.getClientRoles(properties.getResourceClientId()).stream()
                .map(KeycloakRoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void affecterRole(UUID id, String role) {
        adminClient.assignClientRoles(id, properties.getResourceClientId(), List.of(role));
    }

    @Override
    public Optional<GroupeIdentite> groupe(String nom) {
        Optional<KeycloakGroupRepresentation> group = adminClient.getGroupByName(nom);
        if (group.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GroupeIdentite(
                group.get().getId(),
                group.get().getName(),
                group.get().getPath()
        ));
    }

    @Override
    public GroupeIdentite creerGroupe(String nom) {
        UUID parentId = null;
        Optional<GroupeIdentite> parent = adminClient.groupe(properties.getGroupPrefix());
        if (parent.isPresent()) {
            parentId = parent.get().id();
        }
        UUID groupId = adminClient.createGroup(nom, parentId != null ? properties.getGroupPrefix() : null);
        Optional<GroupeIdentite> created = adminClient.groupe(nom);
        return created.map(g -> new GroupeIdentite(g.id(), g.name(), g.path()));
    }

    @Override
    public void supprimerGroupe(String nom) {
        Optional<GroupeIdentite> group = adminClient.groupe(nom);
        group.ifPresent(g -> adminClient.deleteGroup(g.id()));
    }

    @Override
    public List<UUID> membresGroupe(String nom) {
        Optional<GroupeIdentite> group = adminClient.groupe(nom);
        if (group.isEmpty()) {
            return List.of();
        }
        UUID groupId = group.get().id();
        List<KeycloakUserRepresentation> members = adminClient.getGroupMembers(groupId);
        return members.stream()
                .map(KeycloakUserRepresentation::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void supprimerUtilisateursDeGroupe(String nom) {
        List<UUID> members = membresGroupe(nom);
        for (UUID memberId : members) {
            adminClient.deleteUser(memberId);
        }
    }

    private Optional<GroupeIdentite> findGroupByName(String nom) {
        return adminClient.groupe(nom);
    }
}