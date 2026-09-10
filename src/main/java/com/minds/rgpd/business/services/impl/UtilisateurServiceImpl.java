package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.business.services.UtilisateurService;
import com.minds.rgpd.business.utilities.NormaliseurTexte;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UtilisateurServiceImpl implements UtilisateurService {

    private final IdentityGateway identityGateway;
    private final ClientRepository clientRepository;

    @Override
    public Page<UtilisateurDTO> getUtilisateurs(Pageable pageable, UtilisateurFilterCriteria criteria) {
        Pageable requete = pageable == null ? Pageable.unpaged() : pageable;
        UtilisateurFilterCriteria filtres = criteria == null ? UtilisateurFilterCriteria.empty() : criteria;

        Map<String, Client> clients = clientsParNom();
        Set<UUID> identifiants = identifiantsDuClient(filtres.clientId());

        List<IdentiteUtilisateur> trouves = identityGateway.utilisateurs().stream()
                .filter(utilisateur -> identifiants == null || identifiants.contains(utilisateur.id()))
                .filter(utilisateur -> NormaliseurTexte.contient(utilisateur.nom(), filtres.nom()))
                .filter(utilisateur -> NormaliseurTexte.contient(utilisateur.prenom(), filtres.prenom()))
                .sorted(comparateur(requete.getSort()))
                .toList();

        List<IdentiteUtilisateur> page = tronquer(trouves, requete);
        Map<UUID, List<String>> roles = rolesDePage(page);
        List<UtilisateurDTO> contenu = page.stream()
                .map(utilisateur -> versDto(utilisateur, clients,
                        roles.getOrDefault(utilisateur.id(), utilisateur.roles())))
                .toList();

        return new PageImpl<>(contenu, requete, trouves.size());
    }

    @Override
    public UtilisateurDTO getUtilisateur(UUID id) {
        Map<String, Client> clients = clientsParNom();
        return identityGateway.utilisateur(id)
                .map(utilisateur -> versDto(utilisateur, clients, utilisateur.roles()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
    }

    private UtilisateurDTO lectureApresEcriture(UUID id) {
        Map<String, Client> clients = clientsParNom();
        return identityGateway.utilisateur(id)
                .map(utilisateur -> versDto(utilisateur, clients, utilisateur.roles()))
                .orElseThrow(() -> new IdentityProviderException("LECTURE_UTILISATEUR",
                        HttpStatus.BAD_GATEWAY.value(),
                        "l'utilisateur %s est introuvable dans Keycloak après l'écriture".formatted(id)));
    }

    @Override
    public UtilisateurDTO creerUtilisateur(UtilisateurWriteDTO payload) {
        IdentiteCommande commande = versCommande(payload);
        UUID id = identityGateway.creerUtilisateur(commande);
        log.info("Utilisateur Keycloak créé : {} ({})", commande.email(), id);
        return lectureApresEcriture(id);
    }

    @Override
    public UtilisateurDTO modifierUtilisateur(UUID id, UtilisateurWriteDTO payload) {
        identityGateway.utilisateur(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        IdentiteCommande commande = versCommande(payload);
        identityGateway.modifierUtilisateur(id, commande);
        log.info("Utilisateur Keycloak modifié : {}", id);
        return lectureApresEcriture(id);
    }

    @Override
    public void supprimerUtilisateur(UUID id) {
        identityGateway.supprimerUtilisateur(id);
        log.info("Utilisateur Keycloak supprimé : {}", id);
    }

    @Override
    public List<String> getRoles() {
        return identityGateway.rolesDisponibles();
    }

    private IdentiteCommande versCommande(UtilisateurWriteDTO payload) {
        return new IdentiteCommande(
                trim(payload.prenom()),
                trim(payload.nom()),
                trim(payload.email()),
                rolesValides(payload.roles()),
                nomDuClient(payload.clientId()),
                payload.actifOuVrai());
    }

    private List<String> rolesValides(List<String> roles) {
        List<String> demandes = roles == null ? List.of() : roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();

        if (demandes.isEmpty()) {
            throw new IllegalArgumentException("Au moins un rôle est requis");
        }

        List<String> disponibles = identityGateway.rolesDisponibles();
        List<String> canons = new ArrayList<>();
        for (String demande : demandes) {
            String canon = disponibles.stream()
                    .filter(disponible -> NormaliseurTexte.normaliser(disponible).equals(NormaliseurTexte.normaliser(demande)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Le rôle '%s' est inconnu de Keycloak (rôles disponibles : %s)"
                                    .formatted(demande, String.join(", ", disponibles))));
            if (!canons.contains(canon)) {
                canons.add(canon);
            }
        }
        return canons;
    }

    private String nomDuClient(UUID clientId) {
        if (clientId == null) {
            return null;
        }
        return clientRepository.findById(clientId)
                .map(Client::getNom)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));
    }

    private Set<UUID> identifiantsDuClient(UUID clientId) {
        if (clientId == null) {
            return null;
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));
        return new HashSet<>(identityGateway.membresDuGroupe(client.getNom()));
    }

    private Map<String, Client> clientsParNom() {
        Map<String, Client> clients = new LinkedHashMap<>();
        clientRepository.findAll().forEach(client -> clients.put(NormaliseurTexte.normaliser(client.getNom()), client));
        return clients;
    }

    private Map<UUID, List<String>> rolesDePage(List<IdentiteUtilisateur> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        return identityGateway.rolesDesUtilisateurs(page.stream().map(IdentiteUtilisateur::id).toList());
    }

    private UtilisateurDTO versDto(IdentiteUtilisateur utilisateur, Map<String, Client> clients, List<String> roles) {
        Client client = utilisateur.groupe() == null
                ? null
                : clients.get(NormaliseurTexte.normaliser(utilisateur.groupe()));
        return new UtilisateurDTO(
                utilisateur.id(),
                utilisateur.identifiant(),
                utilisateur.prenom(),
                utilisateur.nom(),
                utilisateur.email(),
                utilisateur.actif(),
                roles,
                client == null ? null : client.getId(),
                client == null ? null : client.getNom());
    }

    private static Comparator<IdentiteUtilisateur> comparateur(Sort sort) {
        Comparator<IdentiteUtilisateur> comparateur = null;
        if (sort != null && sort.isSorted()) {
            for (Sort.Order ordre : sort) {
                Comparator<IdentiteUtilisateur> comparateurChamp =
                        Comparator.comparing(extracteur(ordre.getProperty()),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                if (ordre.isDescending()) {
                    comparateurChamp = comparateurChamp.reversed();
                }
                comparateur = comparateur == null ? comparateurChamp : comparateur.thenComparing(comparateurChamp);
            }
        }
        if (comparateur == null) {
            comparateur = Comparator.comparing(IdentiteUtilisateur::nom,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(IdentiteUtilisateur::prenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }
        return comparateur;
    }

    private static Function<IdentiteUtilisateur, String> extracteur(String propriete) {
        return switch (propriete) {
            case "nom" -> IdentiteUtilisateur::nom;
            case "prenom" -> IdentiteUtilisateur::prenom;
            case "email" -> IdentiteUtilisateur::email;
            case "identifiant" -> IdentiteUtilisateur::identifiant;
            case "clientNom" -> IdentiteUtilisateur::groupe;
            default -> throw new IllegalArgumentException(
                    "Le tri sur le champ '%s' n'est pas supporté".formatted(propriete));
        };
    }

    private static List<IdentiteUtilisateur> tronquer(List<IdentiteUtilisateur> utilisateurs, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return utilisateurs;
        }
        int total = utilisateurs.size();
        int debut = (int) Math.min(Math.max(pageable.getOffset(), 0L), total);
        int fin = (int) Math.min(debut + (long) Math.max(pageable.getPageSize(), 0), total);
        return debut >= fin ? List.of() : List.copyOf(utilisateurs.subList(debut, fin));
    }

    private static String trim(String valeur) {
        return valeur == null ? null : valeur.trim();
    }
}
