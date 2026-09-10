package com.minds.rgpd.infrastructure.keycloak;

import com.minds.rgpd.business.exceptions.IdentityProviderIndisponibleException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "application.keycloak", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DesactiveIdentityGateway implements IdentityGateway {

    @Override
    public boolean actif() {
        return false;
    }

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        return List.of();
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateurParEmail(String email) {
        return Optional.empty();
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        throw new IdentityProviderIndisponibleException("création d'utilisateur");
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        throw new IdentityProviderIndisponibleException("modification d'utilisateur");
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        throw new IdentityProviderIndisponibleException("suppression d'utilisateur");
    }

    @Override
    public Map<UUID, List<String>> rolesDesUtilisateurs(Collection<UUID> ids) {
        return Map.of();
    }

    @Override
    public List<String> rolesDisponibles() {
        return List.of();
    }

    @Override
    public List<UUID> membresDuGroupe(String nomGroupe) {
        return List.of();
    }

    @Override
    public void creerGroupe(String nomGroupe) {
        log.debug("Synchronisation Keycloak désactivée : le groupe {} n'est pas créé", nomGroupe);
    }

    @Override
    public void renommerGroupe(String ancienNom, String nouveauNom) {
        log.debug("Synchronisation Keycloak désactivée : le groupe {} n'est pas renommé en {}", ancienNom, nouveauNom);
    }

    @Override
    public void supprimerGroupe(String nomGroupe) {
        log.debug("Synchronisation Keycloak désactivée : le groupe {} n'est pas supprimé", nomGroupe);
    }
}
