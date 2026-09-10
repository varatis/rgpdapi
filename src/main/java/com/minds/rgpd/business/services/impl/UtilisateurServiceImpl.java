package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.exceptions.IdentityProviderException;
import com.minds.rgpd.business.identity.IdentityGateway;
import com.minds.rgpd.business.services.ClientService;
import com.minds.rgpd.business.services.UtilisateurService;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
        List<IdentiteUtilisateur> all = identityGateway.utilisateurs();
        List<UtilisateurDTO> result = all.stream()
                .filter(u -> criteres.nom() == null || u.nom().toLowerCase().contains(criteres.nom().toLowerCase()))
                .filter(u -> criteres.prenom() == null || u.prenom().toLowerCase().contains(criteres.prenom().toLowerCase()))
                .filter(u -> criteres.clientId() == null || u.clientId().equals(criteres.clientId()))
                .map(u -> new UtilisateurDTO(
                        u.id(),
                        u.identifiant(),
                        u.prenom(),
                        u.nom(),
                        u.email(),
                        u.actif(),
                        u.roles(),
                        u.clientId(),
                        u.groupe()
                ))
                .collect(Collectors.toList());
        return result;
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UtilisateurDTO creer(UtilisateurWriteDTO payload) {
        validateRoles(payload.roles());
        Client client = clientRepository.findById(payload.clientId())
                .orElseThrow(() -> new IdentityProviderException("Client introuvable", "id", payload.clientId().toString()));
        IdentiteCommande commande = new IdentiteCommande(
                payload.prenom(),
                payload.nom(),
                payload.email(),
                payload.roles(),
                payload.groupe(),
                payload.actif()
        );
        UUID userId = identityGateway.creerUtilisateur(commande);
        return new UtilisateurDTO(
                userId,
                payload.email(),
                payload.prenom(),
                payload.nom(),
                payload.email(),
                true,
                payload.roles(),
                payload.clientId(),
                client.getNom()
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public UtilisateurDTO modifier(UUID id, UtilisateurWriteDTO payload) {
        validateRoles(payload.roles());
        identityModifierUtilisateur(id, payload);
        Client client = clientRepository.findById(payload.clientId())
                .orElseThrow(() -> new IdentityProviderException("Client introuvable", "id", payload.clientId().toString()));
        return new UtilisateurDTO(
                id,
                payload.email(),
                payload.prenom(),
                payload.nom(),
                payload.email(),
                true,
                payload.roles(),
                payload.clientId(),
                client.getNom()
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public void supprimer(UUID id) {
        identityGateway.supprimerUtilisateur(id);
    }

    @Override
    public UtilisateurDTO getUtilisateurParId(UUID id) {
        return identityGateway.utilisateur(id)
                .map(u -> new UtilisateurDTO(
                        u.id(),
                        u.identifiant(),
                        u.prenom(),
                        u.nom(),
                        u.email(),
                        u.actif(),
                        u.roles(),
                        u.clientId(),
                        u.groupe()
                ))
                .orElseThrow(() -> new IdentityProviderException("Utilisateur introuvable", "id", id.toString()));
    }

    @Override
    public List<String> listerRolesDisponibles() {
        return identityGateway.rolesDisponibles();
    }

    private void validateRoles(List<String> roles) {
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles) {
                if (!role.equals("admin") && !role.equals("user")) {
                    throw new IdentityProviderException("Rôle invalide", "role", role);
                }
            }
        }
    }

    private void identityModifierUtilisateur(UUID id, UtilisateurWriteDTO payload) {
        identityGateway.modifierUtilisateur(id, new com.minds.rgpd.business.identity.IdentiteCommande(
                payload.prenom(),
                payload.nom(),
                payload.email(),
                payload.roles(),
                payload.groupe(),
                payload.actif()
        ));
    }
}