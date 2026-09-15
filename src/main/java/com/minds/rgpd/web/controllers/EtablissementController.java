package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import com.minds.rgpd.business.services.EtablissementService;
import com.minds.rgpd.business.utilities.AccessUserInformation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import java.util.UUID;

@RestController
@RequestMapping("/etablissements")
@RequiredArgsConstructor
@Validated
@Tag(name = "Etablissement Controller", description = "Gère les entités Etablissement")
public class EtablissementController {

    private final EtablissementService etablissementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste paginée et filtrée des établissements du client de l'utilisateur connecté")
    public ResponseEntity<Page<EtablissementDTO>> getEtablissements(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "nom") Pageable pageable,
            @RequestParam(required = false) @Size(max = 255) String nom,
            @RequestParam(required = false) @Size(max = 3) String departement,
            @RequestParam(required = false) Boolean principal) {
        // Le client vient du jeton : l'écran est réservé aux comptes rattachés
        // à un seul client, personne ne choisit le registre qu'il consulte.
        String clientNom = AccessUserInformation.getClientUniqueDuJeton(jwt);
        EtablissementFilterCriteria criteria = new EtablissementFilterCriteria(nom, departement, principal);
        return ResponseEntity.ok(etablissementService.getEtablissements(pageable, clientNom, criteria));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détail d'un établissement")
    public ResponseEntity<EtablissementDTO> getEtablissement(@PathVariable UUID id) {
        return ResponseEntity.ok(etablissementService.getOneEtablissement(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Crée un établissement")
    public ResponseEntity<EtablissementDTO> postEtablissement(@RequestBody @Valid EtablissementDTO etablissement) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(etablissementService.createEtablissement(etablissement));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Met à jour un établissement identifié par son UUID")
    public ResponseEntity<EtablissementDTO> putEtablissement(
            @PathVariable UUID id,
            @RequestBody @Valid EtablissementDTO etablissement) {
        return ResponseEntity.ok(etablissementService.updateEtablissement(id, etablissement));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    @Operation(summary = "Supprime un établissement, s'il n'est rattaché à aucun traitement")
    public ResponseEntity<Void> deleteEtablissement(@PathVariable UUID id) {
        etablissementService.deleteEtablissementById(id);
        return ResponseEntity.noContent().build();
    }
}
