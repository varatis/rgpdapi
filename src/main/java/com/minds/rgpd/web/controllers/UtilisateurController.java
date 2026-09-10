package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.dtos.UtilisateurFilterCriteria;
import com.minds.rgpd.business.dtos.UtilisateurWriteDTO;
import com.minds.rgpd.business.services.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Utilisateur Controller", description = "Gère les identités et les rôles, en direct avec Keycloak")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Recherche les utilisateurs par nom, prénom et client")
    public ResponseEntity<Page<UtilisateurDTO>> getUtilisateurs(
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) @Size(max = 255) String nom,
            @RequestParam(required = false) @Size(max = 255) String prenom,
            @RequestParam(required = false) UUID clientId) {
        UtilisateurFilterCriteria criteria = new UtilisateurFilterCriteria(nom, prenom, clientId);
        return ResponseEntity.ok(utilisateurService.getUtilisateurs(pageable, criteria));
    }

    @GetMapping("/roles")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Rôles applicatifs proposés à l'affectation, lus depuis Keycloak")
    public ResponseEntity<List<String>> getRoles() {
        return ResponseEntity.ok(utilisateurService.getRoles());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'un utilisateur")
    public ResponseEntity<UtilisateurDTO> getUtilisateur(@PathVariable UUID id) {
        return ResponseEntity.ok(utilisateurService.getUtilisateur(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Crée l'utilisateur dans Keycloak, avec son mot de passe temporaire et ses rôles")
    public ResponseEntity<UtilisateurDTO> creerUtilisateur(@Valid @RequestBody UtilisateurWriteDTO payload) {
        UtilisateurDTO cree = utilisateurService.creerUtilisateur(payload);
        return ResponseEntity.created(URI.create("/utilisateurs/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Modifie un utilisateur : identité, rôles et client de rattachement")
    public ResponseEntity<UtilisateurDTO> modifierUtilisateur(@PathVariable UUID id,
                                                              @Valid @RequestBody UtilisateurWriteDTO payload) {
        return ResponseEntity.ok(utilisateurService.modifierUtilisateur(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Supprime définitivement l'utilisateur de Keycloak")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable UUID id) {
        utilisateurService.supprimerUtilisateur(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
