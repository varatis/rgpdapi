package com.minds.rgpd.business.identity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IdentityGateway {

    boolean actif();

    List<IdentiteUtilisateur> utilisateurs();

    Optional<IdentiteUtilisateur> utilisateur(UUID id);

    Optional<IdentiteUtilisateur> utilisateurParEmail(String email);

    Map<UUID, List<String>> rolesDesUtilisateurs(Collection<UUID> ids);

    UUID creerUtilisateur(IdentiteCommande commande);

    void modifierUtilisateur(UUID id, IdentiteCommande commande);

    void supprimerUtilisateur(UUID id);

    List<String> rolesDisponibles();

    List<UUID> membresDuGroupe(String nomGroupe);

    void creerGroupe(String nomGroupe);

    void renommerGroupe(String ancienNom, String nouveauNom);

    void supprimerGroupe(String nomGroupe);
}
