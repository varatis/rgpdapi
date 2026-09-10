package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.DuplicateResourceException;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakClientRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakGroupRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakRoleRepresentation;
import com.minds.rgpd.infrastructure.keycloak.dto.KeycloakUserRepresentation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "application.keycloak", name = "enabled", havingValue = "true")
public class KeycloakIdentityGateway implements IdentityGateway {

    private static final ParameterizedTypeReference<KeycloakUserRepresentation> UTILISATEUR =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<KeycloakRoleRepresentation> ROLE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<KeycloakUserRepresentation>> LISTE_UTILISATEURS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<KeycloakGroupRepresentation>> LISTE_GROUPES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<KeycloakRoleRepresentation>> LISTE_ROLES =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<KeycloakClientRepresentation>> LISTE_CLIENTS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<Map<String, Object>> UTILISATEUR_BRUT =
            new ParameterizedTypeReference<>() {
            };

    private static final String CARACTERES_MOT_DE_PASSE =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789#$%*+-";
    private static final int LONGUEUR_MOT_DE_PASSE = 18;

    private final KeycloakProperties properties;
    private final KeycloakAdminClient client;
    private final SecureRandom aleatoire = new SecureRandom();

    private String uuidClient;

    public KeycloakIdentityGateway(KeycloakProperties properties) {
        this(properties, new KeycloakAdminClient(properties));
    }

    KeycloakIdentityGateway(KeycloakProperties properties, KeycloakAdminClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public boolean actif() {
        return true;
    }

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        return pager("/users", "briefRepresentation=false", LISTE_UTILISATEURS).stream()
                .map(utilisateur -> versIdentite(utilisateur, List.of()))
                .toList();
    }

    @Override
    public Map<UUID, List<String>> rolesDesUtilisateurs(Collection<UUID> ids) {
        Map<UUID, List<String>> roles = new LinkedHashMap<>();
        if (ids == null) {
            return roles;
        }
        ids.stream().filter(Objects::nonNull).forEach(id -> roles.put(id, rolesDeUtilisateur(id)));
        return roles;
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return lireUtilisateur(id).map(utilisateur -> versIdentite(utilisateur, rolesDeUtilisateur(id)));
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateurParEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return client.lire("/users?email=%s&exact=true".formatted(KeycloakAdminClient.encoder(email)), LISTE_UTILISATEURS)
                .orElse(List.of())
                .stream()
                .filter(utilisateur -> email.equalsIgnoreCase(utilisateur.email()))
                .findFirst()
                .map(utilisateur -> versIdentite(utilisateur, List.of()));
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        UUID id = null;
        try {
            id = client.creer("/users", corpsCreation(commande))
                    .map(KeycloakIdentityGateway::versUuid)
                    .orElseThrow(() -> new IdentityProviderException("CREATION_UTILISATEUR",
                            HttpStatus.BAD_GATEWAY.value(), "Keycloak n'a renvoyé aucun identifiant d'utilisateur"));
            ajouterRoles(id, commande.roles());
            rejoindreGroupe(id, commande.groupe());
            return id;
        } catch (IdentityProviderException e) {
            supprimerSilencieusement(id);
            if (e.conflit()) {
                throw new DuplicateResourceException("Utilisateur", "email", commande.email());
            }
            throw e;
        } catch (RuntimeException e) {
            supprimerSilencieusement(id);
            throw e;
        }
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        Map<String, Object> utilisateur = lireUtilisateurBrut(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        utilisateur.remove("groups");
        utilisateur.remove("access");
        utilisateur.put("firstName", commande.prenom());
        utilisateur.put("lastName", commande.nom());
        utilisateur.put("enabled", commande.actif());
        if (commande.email() != null && !commande.email().isBlank()) {
            utilisateur.put("email", commande.email());
            utilisateur.put("username", commande.email());
        }

        client.modifier("/users/" + id, utilisateur);
        remplacerRoles(id, commande.roles());
        rejoindreGroupe(id, commande.groupe());
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        lireUtilisateurBrut(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        client.supprimer("/users/" + id);
    }

    @Override
    public List<String> rolesDisponibles() {
        Map<String, String> parCleInsensible = new LinkedHashMap<>();
        for (KeycloakRoleRepresentation role : rolesDuClient()) {
            if (role.name() != null) {
                parCleInsensible.putIfAbsent(role.name().toLowerCase(Locale.ROOT), role.name());
            }
        }
        return List.copyOf(parCleInsensible.values());
    }

    @Override
    public List<UUID> membresDuGroupe(String nomGroupe) {
        return groupe(nomGroupe)
                .map(groupe -> membres(groupe.id()))
                .orElse(List.of());
    }

    @Override
    public void creerGroupe(String nomGroupe) {
        if (groupe(nomGroupe).isPresent()) {
            return;
        }
        groupeOuCreation(nomGroupe);
    }

    @Override
    public void renommerGroupe(String ancienNom, String nouveauNom) {
        KeycloakGroupRepresentation existant = groupe(ancienNom).orElse(null);
        if (existant == null) {
            creerGroupe(nouveauNom);
            return;
        }
        if (groupe(nouveauNom).isPresent()) {
            throw new DuplicateResourceException("Groupe", "nom", nouveauNom);
        }
        client.modifier("/groups/" + existant.id(), Map.of("id", existant.id(), "name", nouveauNom));
    }

    @Override
    public void supprimerGroupe(String nomGroupe) {
        groupe(nomGroupe).ifPresent(groupe -> client.supprimer("/groups/" + groupe.id()));
    }

    private IdentiteUtilisateur versIdentite(KeycloakUserRepresentation utilisateur, List<String> roles) {
        return new IdentiteUtilisateur(versUuid(utilisateur.id()), utilisateur.username(), utilisateur.firstName(),
                utilisateur.lastName(), utilisateur.email(), utilisateur.actif(), roles, nomGroupeClient(utilisateur));
    }

    private Optional<KeycloakUserRepresentation> lireUtilisateur(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return client.lire("/users/" + id, UTILISATEUR);
    }

    private Optional<Map<String, Object>> lireUtilisateurBrut(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return client.lire("/users/" + id, UTILISATEUR_BRUT);
    }

    private Map<String, Object> corpsCreation(IdentiteCommande commande) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("username", commande.email());
        corps.put("email", commande.email());
        corps.put("firstName", commande.prenom());
        corps.put("lastName", commande.nom());
        corps.put("enabled", commande.actif());
        corps.put("emailVerified", false);
        corps.put("requiredActions", List.of("UPDATE_PASSWORD"));
        Map<String, Object> credential = new LinkedHashMap<>();
        credential.put("type", "password");
        credential.put("value", motDePasseTemporaire());
        credential.put("temporary", true);
        corps.put("credentials", List.of(credential));
        return corps;
    }

    private String motDePasseTemporaire() {
        StringBuilder motDePasse = new StringBuilder(LONGUEUR_MOT_DE_PASSE);
        for (int index = 0; index < LONGUEUR_MOT_DE_PASSE; index++) {
            motDePasse.append(CARACTERES_MOT_DE_PASSE.charAt(aleatoire.nextInt(CARACTERES_MOT_DE_PASSE.length())));
        }
        return motDePasse.toString();
    }

    private void supprimerSilencieusement(UUID id) {
        if (id == null) {
            return;
        }
        try {
            client.supprimer("/users/" + id);
        } catch (RuntimeException e) {
            log.warn("L'utilisateur Keycloak {} a été créé puis la synchronisation a échoué, " +
                    "la suppression compensatoire n'a pas abouti : {}", id, e.getMessage());
        }
    }

    private void ajouterRoles(UUID id, List<String> roles) {
        List<KeycloakRoleRepresentation> rolesALouer = versRoles(roles);
        if (!rolesALouer.isEmpty()) {
            client.ajouter(cheminRoles(id), rolesALouer);
        }
    }

    private void remplacerRoles(UUID id, List<String> roles) {
        List<KeycloakRoleRepresentation> courants = client.lire(cheminRoles(id), LISTE_ROLES).orElse(List.of());
        Set<String> demandes = new LinkedHashSet<>(roles);

        List<KeycloakRoleRepresentation> aAjouter = demandes.stream()
                .filter(demande -> courants.stream().noneMatch(courant -> demande.equalsIgnoreCase(courant.name())))
                .map(this::roleOuErreur)
                .toList();

        List<KeycloakRoleRepresentation> aRetirer = courants.stream()
                .filter(courant -> demandes.stream().noneMatch(demande -> demande.equalsIgnoreCase(courant.name())))
                .toList();

        if (!aAjouter.isEmpty()) {
            client.ajouter(cheminRoles(id), aAjouter);
        }
        if (!aRetirer.isEmpty()) {
            client.supprimer(cheminRoles(id), aRetirer);
        }
    }

    private String cheminRoles(UUID id) {
        return "/users/%s/role-mappings/clients/%s".formatted(id, uuidDuClient());
    }

    private List<KeycloakRoleRepresentation> versRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(Objects::nonNull)
                .map(role -> role.trim())
                .filter(role -> !role.isEmpty())
                .map(this::roleOuErreur)
                .toList();
    }

    private KeycloakRoleRepresentation roleOuErreur(String nom) {
        return client.lire("/clients/%s/roles/%s".formatted(uuidDuClient(), KeycloakAdminClient.encoder(nom)), ROLE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le rôle Keycloak '%s' est introuvable dans le client %s"
                                .formatted(nom, properties.getResourceClientId())));
    }

    private List<KeycloakRoleRepresentation> rolesDuClient() {
        return client.lire("/clients/%s/roles?briefRepresentation=true".formatted(uuidDuClient()), LISTE_ROLES)
                .orElse(List.of());
    }

    private List<String> rolesDeUtilisateur(UUID id) {
        if (id == null) {
            return List.of();
        }
        Map<String, String> parCleInsensible = new LinkedHashMap<>();
        client.lire(cheminRoles(id), LISTE_ROLES).orElse(List.of()).stream()
                .map(KeycloakRoleRepresentation::name)
                .filter(Objects::nonNull)
                .forEach(nom -> parCleInsensible.putIfAbsent(nom.toLowerCase(Locale.ROOT), nom));
        return List.copyOf(parCleInsensible.values());
    }

    private String uuidDuClient() {
        String uuid = uuidClient;
        if (uuid == null) {
            synchronized (this) {
                if (uuidClient == null) {
                    uuidClient = client.lire("/clients?clientId=%s"
                                    .formatted(KeycloakAdminClient.encoder(properties.getResourceClientId())), LISTE_CLIENTS)
                            .orElse(List.of())
                            .stream()
                            .map(KeycloakClientRepresentation::id)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElseThrow(() -> new IdentityProviderException("LECTURE_CLIENT",
                                    HttpStatus.NOT_FOUND.value(),
                                    "le client Keycloak '%s' est introuvable dans le realm '%s'"
                                            .formatted(properties.getResourceClientId(), properties.getRealm())));
                }
                uuid = uuidClient;
            }
        }
        return uuid;
    }

    private void rejoindreGroupe(UUID id, String nomGroupe) {
        if (nomGroupe == null || nomGroupe.isBlank()) {
            return;
        }
        KeycloakGroupRepresentation cible = groupeOuCreation(nomGroupe);
        KeycloakUserRepresentation courant = lireUtilisateur(id).orElse(null);
        if (courant != null && courant.groups() != null) {
            courant.groups().stream()
                    .filter(KeycloakIdentityGateway::groupeRacine)
                    .filter(groupe -> !nomGroupe.equals(groupe.name()))
                    .filter(groupe -> groupe.id() != null && !groupe.id().equals(cible.id()))
                    .forEach(groupe -> client.supprimer("/users/%s/groups/%s".formatted(id, groupe.id())));
        }
        if (courant == null || courant.groups() == null || courant.groups().stream()
                .noneMatch(groupe -> nomGroupe.equals(groupe.name()) && groupeRacine(groupe))) {
            client.modifier("/users/%s/groups/%s".formatted(id, cible.id()), null);
        }
    }

    private KeycloakGroupRepresentation groupeOuCreation(String nomGroupe) {
        return groupe(nomGroupe).orElseGet(() -> {
            String id = client.creer("/groups", Map.of("name", nomGroupe))
                    .orElseThrow(() -> new IdentityProviderException("CREATION_GROUPE", HttpStatus.BAD_GATEWAY.value(),
                            "Keycloak n'a renvoyé aucun identifiant de groupe pour %s".formatted(nomGroupe)));
            log.info("Groupe Keycloak rattaché au client {} créé", nomGroupe);
            return new KeycloakGroupRepresentation(id, nomGroupe, "/" + nomGroupe);
        });
    }

    private Optional<KeycloakGroupRepresentation> groupe(String nomGroupe) {
        if (nomGroupe == null || nomGroupe.isBlank()) {
            return Optional.empty();
        }
        return client.lire("/groups?search=%s&exact=true".formatted(KeycloakAdminClient.encoder(nomGroupe)), LISTE_GROUPES)
                .orElse(List.of())
                .stream()
                .filter(groupe -> nomGroupe.equals(groupe.name()))
                .findFirst();
    }

    private List<UUID> membres(String idGroupe) {
        return pager("/groups/%s/members".formatted(idGroupe), "briefRepresentation=true", LISTE_UTILISATEURS).stream()
                .map(KeycloakUserRepresentation::id)
                .map(KeycloakIdentityGateway::versUuid)
                .filter(Objects::nonNull)
                .toList();
    }

    private <T> List<T> pager(String chemin, String parametres, ParameterizedTypeReference<List<T>> type) {
        List<T> resultat = new ArrayList<>();
        int taillePage = Math.max(properties.getTaillePage(), 1);
        int maximum = Math.max(properties.getNombreMaxResultats(), taillePage);
        int premier = 0;
        while (resultat.size() < maximum) {
            List<T> page = client.lire(cheminPage(chemin, parametres, premier, taillePage), type).orElse(List.of());
            resultat.addAll(page);
            if (page.size() < taillePage) {
                return resultat;
            }
            premier += page.size();
        }
        log.warn("Le seuil de {} éléments est atteint sur {}, la liste renvoyée est tronquée", maximum, chemin);
        return resultat;
    }

    private static String cheminPage(String chemin, String parametres, int premier, int taillePage) {
        StringBuilder url = new StringBuilder(chemin).append("?first=").append(premier).append("&max=").append(taillePage);
        if (parametres != null && !parametres.isBlank()) {
            url.append('&').append(parametres);
        }
        return url.toString();
    }

    private static boolean groupeRacine(KeycloakGroupRepresentation groupe) {
        String chemin = groupe.path();
        return chemin == null || chemin.indexOf('/', 1) < 0;
    }

    private static String nomGroupeClient(KeycloakUserRepresentation utilisateur) {
        if (utilisateur.groups() == null) {
            return null;
        }
        return utilisateur.groups().stream()
                .filter(KeycloakIdentityGateway::groupeRacine)
                .map(KeycloakGroupRepresentation::name)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    static UUID versUuid(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valeur);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
