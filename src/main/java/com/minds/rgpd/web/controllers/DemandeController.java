package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.business.services.DemandeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
@Tag(name = "Demande Controller", description = "Gestion des demandes")
public class DemandeController {

    private final DemandeService demandeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeDTO>> getDemandes() {

        return ResponseEntity.ok(
                demandeService.getDemandes()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeDTO> getDemande(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                demandeService.getDemande(id)
        );
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeDTO> createDemande(
            @RequestBody DemandeDTO demandeDTO) {
        return ResponseEntity.ok(
                demandeService.createDemande(demandeDTO)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Met à jour une demande identifiée par son UUID")
    public ResponseEntity<DemandeDTO> updateDemande(
            @PathVariable UUID id,
            @RequestBody DemandeDTO demandeDTO) {
        return ResponseEntity.ok(
                demandeService.updateDemande(id, demandeDTO)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprime une demande identifiée par son UUID")
    public ResponseEntity<Void> deleteDemande(@PathVariable UUID id) {
        demandeService.deleteDemandeById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/traiter")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<DemandeDTO> traiterDemande(@PathVariable UUID id) {
        return ResponseEntity.ok(
                demandeService.traiterDemande(id)
        );
    }
}
