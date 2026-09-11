package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/utilisateurs")
@Tag(name = "Utilisateur Controller", description = "Gère les utilisateurs via Keycloak")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des utilisateurs avec filtres")
    public ResponseEntity<List<UtilisateurDTO>> rechercher(
            Pageable pageable,
            UtilisateurFilterCriteria criteres) {
        List<UtilisateurDTO> result = utilisateurService.rechercher(criteres);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/roles")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des rôles disponibles")
    public ResponseEntity<List<String>> listerRoles() {
        return ResponseEntity.ok(utilisateurService.listerRolesDisponibles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'un utilisateur")
    public ResponseEntity<UtilisateurDTO> getUtilisateurParId(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.getUtilisateurParId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @Operation(summary = "Crée un utilisateur")
    public ResponseEntity<UtilisateurDTO> creer(@Valid @RequestBody UtilisateurWriteDTO payload) {
        return ResponseEntity.status(201).body(utilisateurService.creer(payload));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @Operation(summary = "Modifie un utilisateur")
    public ResponseEntity<UtilisateurDTO> modifier(@PathVariable UUID id, @Valid @RequestBody UtilisateurWriteDTO payload) {
        return ResponseEntity.ok(utilisateurService.modifier(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    @Operation(summary = "Supprime un utilisateur")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id) {
        utilisateurService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}