package com.minds.rgpd.identity;

import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FakeIdentityGateway implements IdentityGateway {

    private final Map<UUID, IdentiteUtilisateur> utilisateurs = new LinkedHashMap<>();
    private final Map<String, List<UUID>> membresParGroupe = new LinkedHashMap<>();
    private final Set<String> groupes = new LinkedHashSet<>();
    private final List<String> roles = new ArrayList<>(List.of("admin", "user"));

    private final List<UUID> utilisateursSupprimes = new ArrayList<>();
    private final List<String> groupesCrees = new ArrayList<>();
    private final List<String> groupesSupprimes = new ArrayList<>();
    private final List<IdentiteCommande> commandesRecues = new ArrayList<>();

    @Override
    public boolean actif() {
        return true;
    }

    @Override
    public List<IdentiteUtilisateur> utilisateurs() {
        return List.copyOf(utilisateurs.values());
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateur(UUID id) {
        return Optional.ofNullable(utilisateurs.get(id));
    }

    @Override
    public Optional<IdentiteUtilisateur> utilisateurParEmail(String email) {
        return utilisateurs.values().stream()
                .filter(utilisateur -> email == null || email.equalsIgnoreCase(utilisateur.email()))
                .findFirst();
    }

    @Override
    public UUID creerUtilisateur(IdentiteCommande commande) {
        commandesRecues.add(commande);
        UUID id = UUID.randomUUID();
        enregistrer(id, commande);
        return id;
    }

    @Override
    public void modifierUtilisateur(UUID id, IdentiteCommande commande) {
        commandesRecues.add(commande);
        if (!utilisateurs.containsKey(id)) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }
        enregistrer(id, commande);
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        if (utilisateurs.remove(id) == null) {
            throw new ResourceNotFoundException("Utilisateur", "id", id);
        }
        utilisateursSupprimes.add(id);
        membresParGroupe.values().forEach(membres -> membres.remove(id));
    }

    @Override
    public Map<UUID, List<String>> rolesDesUtilisateurs(Collection<UUID> ids) {
        Map<UUID, List<String>> roles = new LinkedHashMap<>();
        if (ids == null) {
            return roles;
        }
        ids.stream()
                .filter(java.util.Objects::nonNull)
                .forEach(id -> Optional.ofNullable(utilisateurs.get(id))
                        .ifPresent(utilisateur -> roles.put(id, utilisateur.roles())));
        return roles;
    }

    @Override
    public List<String> rolesDisponibles() {
        return List.copyOf(roles);
    }

    @Override
    public List<UUID> membresDuGroupe(String nomGroupe) {
        return List.copyOf(membresParGroupe.getOrDefault(nomGroupe, List.of()));
    }

    @Override
    public void creerGroupe(String nomGroupe) {
        groupes.add(nomGroupe);
        groupesCrees.add(nomGroupe);
    }

    @Override
    public void renommerGroupe(String ancienNom, String nouveauNom) {
        groupes.remove(ancienNom);
        groupes.add(nouveauNom);
        List<UUID> membres = membresParGroupe.remove(ancienNom);
        membresParGroupe.put(nouveauNom, membres == null ? new ArrayList<>() : membres);
        utilisateurs.keySet().forEach(id -> {
            IdentiteUtilisateur utilisateur = utilisateurs.get(id);
            if (utilisateur != null && ancienNom.equals(utilisateur.groupe())) {
                utilisateurs.put(id, new IdentiteUtilisateur(utilisateur.id(), utilisateur.identifiant(),
                        utilisateur.prenom(), utilisateur.nom(), utilisateur.email(), utilisateur.actif(),
                        utilisateur.roles(), nouveauNom));
            }
        });
    }

    @Override
    public void supprimerGroupe(String nomGroupe) {
        groupes.remove(nomGroupe);
        groupesSupprimes.add(nomGroupe);
        membresParGroupe.remove(nomGroupe);
    }

    public void ajouterRole(String role) {
        roles.add(role);
    }

    public void creerGroupeSilencieusement(String nomGroupe) {
        groupes.add(nomGroupe);
    }

    public int nombreUtilisateurs() {
        return utilisateurs.size();
    }

    public List<UUID> utilisateursSupprimes() {
        return List.copyOf(utilisateursSupprimes);
    }

    public List<String> groupesCrees() {
        return List.copyOf(groupesCrees);
    }

    public List<String> groupesSupprimes() {
        return List.copyOf(groupesSupprimes);
    }

    public List<IdentiteCommande> commandesRecues() {
        return List.copyOf(commandesRecues);
    }

    public Set<String> groupes() {
        return Set.copyOf(groupes);
    }

    public void reinitialiser() {
        utilisateurs.clear();
        membresParGroupe.clear();
        groupes.clear();
        utilisateursSupprimes.clear();
        groupesCrees.clear();
        groupesSupprimes.clear();
        commandesRecues.clear();
    }

    private void enregistrer(UUID id, IdentiteCommande commande) {
        utilisateurs.put(id, new IdentiteUtilisateur(id, commande.email(), commande.prenom(), commande.nom(),
                commande.email(), commande.actif(), commande.roles(), commande.groupe()));
        membresParGroupe.values().forEach(membres -> membres.remove(id));
        if (commande.groupe() != null) {
            groupes.add(commande.groupe());
            membresParGroupe.computeIfAbsent(commande.groupe(), groupe -> new ArrayList<>()).add(id);
        }
    }
}
