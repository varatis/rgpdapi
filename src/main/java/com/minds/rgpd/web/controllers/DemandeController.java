package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.DemandeDTO;
import com.minds.rgpd.business.services.DemandeService;
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
}
