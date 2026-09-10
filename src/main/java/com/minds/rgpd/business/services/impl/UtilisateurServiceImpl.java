package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.business.identity.IdentiteUtilisateur;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.business.services.UtilisateurService;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UtilisateurServiceImpl implements UtilisateurService {

    private final IdentityGateway identityGateway;

    private final ClientRepository clientRepository;

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public List<UtilisateurDTO> rechercher(UtilisateurFilterCriteria criteres) {
        Map<String, Client> clientsParNom = clientsParNom();
        return identityGateway.utilisateurs().stream()
                .filter(u -> criteres.nom() == null || u.nom().toLowerCase().contains(criteres.nom().toLowerCase()))
                .filter(u -> criteres.prenom() == null || u.prenom().toLowerCase().contains(criteres.prenom().toLowerCase()))
                .filter(u -> criteres.clientId() == null
                        || criteres.clientId().equals(clientIdDuGroupe(u, clientsParNom)))
                .map(u -> toDTO(u, clientsParNom))
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UtilisateurDTO creer(UtilisateurWriteDTO payload) {
        validateRoles(payload.roles());
        Client client = client(payload.clientId());
        IdentiteCommande commande = new IdentiteCommande(
                payload.prenom(),
                payload.nom(),
                payload.email(),
                payload.roles(),
                payload.groupe(),
                actifParDefaut(payload.actif())
        );
        UUID userId = identityGateway.creerUtilisateur(commande);
        return toDTO(userId, payload, client);
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UtilisateurDTO modifier(UUID id, UtilisateurWriteDTO payload) {
        validateRoles(payload.roles());
        Client client = client(payload.clientId());
        identityGateway.modifierUtilisateur(id, new IdentiteCommande(
                payload.prenom(),
                payload.nom(),
                payload.email(),
                payload.roles(),
                payload.groupe(),
                actifParDefaut(payload.actif())
        ));
        return toDTO(id, payload, client);
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public void supprimer(UUID id) {
        identityGateway.supprimerUtilisateur(id);
    }

    @Override
    public UtilisateurDTO getUtilisateurParId(UUID id) {
        Map<String, Client> clientsParNom = clientsParNom();
        return identityGateway.utilisateur(id)
                .map(u -> toDTO(u, clientsParNom))
                .orElseThrow(() -> new IdentityProviderException("Utilisateur introuvable", "id", id.toString()));
    }

    @Override
    public List<String> listerRolesDisponibles() {
        return identityGateway.rolesDisponibles();
    }

    /**
     * Un utilisateur peut ne dépendre d'aucun client (ex. superadmin) :
     * {@code clientId} null est légal — {@code findById(null)} étant de plus
     * rejeté par Spring Data, il ne faut l'appeler que si l'identifiant existe.
     */
    private Client client(UUID clientId) {
        if (clientId == null) {
            return null;
        }
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new IdentityProviderException("Client introuvable", "id", clientId.toString()));
    }

    private UtilisateurDTO toDTO(UUID id, UtilisateurWriteDTO payload, Client client) {
        return new UtilisateurDTO(
                id,
                payload.email(),
                payload.prenom(),
                payload.nom(),
                payload.email(),
                actifParDefaut(payload.actif()),
                payload.roles(),
                client != null ? client.getId() : null,
                client != null ? client.getNom() : payload.groupe()
        );
    }

    /**
     * Les rôles acceptés sont ceux que le fournisseur d'identité expose
     * réellement (cf. {@link IdentityGateway#rolesDisponibles()}) : côté
     * Keycloak ils sont en minuscules (« admin », « user ») et le
     * JwtAuthConverter les normalise en ROLE_ADMIN, ROLE_USER pour
     * hasAnyRole(...).
     */
    private void validateRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }
        List<String> disponibles = identityGateway.rolesDisponibles();
        for (String role : roles) {
            if (!disponibles.contains(role)) {
                throw new IdentityProviderException("Rôle invalide", "role", role);
            }
        }
    }

    /**
     * Le groupe Keycloak d'un utilisateur porte le nom de son client : c'est
     * par lui que le rattachement utilisateur ↔ client est reconstitué.
     */
    private UtilisateurDTO toDTO(IdentiteUtilisateur utilisateur, Map<String, Client> clientsParNom) {
        Client client = clientDuGroupe(utilisateur, clientsParNom);
        return new UtilisateurDTO(
                utilisateur.id(),
                utilisateur.identifiant(),
                utilisateur.prenom(),
                utilisateur.nom(),
                utilisateur.email(),
                utilisateur.actif(),
                utilisateur.roles(),
                client != null ? client.getId() : null,
                client != null ? client.getNom() : utilisateur.groupe()
        );
    }

    private UUID clientIdDuGroupe(IdentiteUtilisateur utilisateur, Map<String, Client> clientsParNom) {
        Client client = clientDuGroupe(utilisateur, clientsParNom);
        return client != null ? client.getId() : null;
    }

    private Client clientDuGroupe(IdentiteUtilisateur utilisateur, Map<String, Client> clientsParNom) {
        return utilisateur.groupe() != null ? clientsParNom.get(utilisateur.groupe()) : null;
    }

    private Map<String, Client> clientsParNom() {
        return clientRepository.findAll().stream()
                .collect(Collectors.toMap(Client::getNom, client -> client, (premier, second) -> premier));
    }

    private boolean actifParDefaut(Boolean actif) {
        return actif == null || actif;
    }
}
