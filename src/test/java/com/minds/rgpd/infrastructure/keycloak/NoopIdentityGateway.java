package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.identity.GroupeIdentite;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Passerelle d'identité de substitution pour les tests d'intégration :
 * Keycloak y est désactivé (voir {@code application-test.yaml}), cette
 * implémentation restitue le référentiel d'utilisateurs qui était auparavant
 * semé par le script SQL d'initialisation.
 */
@Component
@Profile("test")
@Primary
public class NoopIdentityGateway implements IdentityGateway {

    private static final List<IdentiteUtilisateur> UTILISATEURS = List.of(
            new IdentiteUtilisateur(
                    UUID.fromString("d6dfd117-8047-4a9a-afca-f5268a38bfcf"),
                    "alice@alpha.com",
                    "Alice",
                    "Dupont",
                    "alice@alpha.com",
                    true,
                    List.of("user"),
                    "La breteche"),
            new IdentiteUtilisateur(
                    UUID.fromString("6a04222b-60f8-434b-bdff-c01ce36fde2f"),
                    "bob@alpha.com",
                    "Bob",
                    "Martin",
                    "bob@alpha.com",
                    true,
                    List.of("user"),
                    "La breteche"),
            new IdentiteUtilisateur(
                    UUID.fromString("e9048a22-e73d-4b35-b08a-0540c58e7a6f"),
                    "claire@beta.com",
                    "Claire",
                    "Durand",
                    "claire@beta.com",
                    true,
                    List.of("admin"),
                    "Entreprise Alpha"));

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        return UTILISATEURS;
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return UTILISATEURS.stream()
                .filter(u -> u.id().equals(id))
                .findFirst();
    }

    @Override
    public Optional<IdentiteUtilisateur> parEmail(String email) {
        return UTILISATEURS.stream()
                .filter(u -> u.email().equals(email))
                .findFirst();
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        return UUID.randomUUID();
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        // sans effet en test
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        // sans effet en test
    }

    @Override
    public void definirMotDePasse(UUID id, String motDePasse) {
        // sans effet en test
    }

    @Override
    public List<String> rolesDisponibles() {
        return List.of("admin", "user");
    }

    @Override
    public void affecterRole(UUID id, String role) {
        // sans effet en test
    }

    @Override
    public Optional<GroupeIdentite> groupe(String nom) {
        return Optional.empty();
    }

    @Override
    public GroupeIdentite creerGroupe(String nom) {
        return new GroupeIdentite(UUID.randomUUID(), nom, "/clients/" + nom);
    }

    @Override
    public void supprimerGroupe(String nom) {
        // sans effet en test
    }

    @Override
    public List<UUID> membresGroupe(String nom) {
        return List.of();
    }

    @Override
    public void supprimerUtilisateursDeGroupe(String nom) {
        // sans effet en test
    }
}
