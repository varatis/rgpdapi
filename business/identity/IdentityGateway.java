package com.minds.rgpd.business.identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityGateway {
    List<IdentiteUtilisateur> utilisateurs();

    Optional<IdentiteUtilisateur> utilisateur(UUID id);

    Optional<IdentiteUtilisateur> parEmail(String email);

    UUID creerUtilisateur(IdentiteCommande commande);

    void modifierUtilisateur(UUID id, IdentiteCommande commande);

    void supprimerUtilisateur(UUID id);

    /**
     * Définit un mot de passe temporaire : l'utilisateur devra le remplacer
     * lors de sa première connexion.
     */
    void definirMotDePasse(UUID utilisateurId, String motDePasse);

    List<String> rolesDisponibles();

    void affecterRole(UUID id, String role);

    /** Groupe de client par nom : sous-groupe direct du parent configuré. */
    Optional<GroupeIdentite> groupe(String nom);

    /** Crée le groupe de client sous le parent configuré (jamais à la racine). */
    GroupeIdentite creerGroupe(String nom);

    void supprimerGroupe(String nom);

    List<UUID> membresGroupe(String nom);

    void supprimerUtilisateursDeGroupe(String nom);
}