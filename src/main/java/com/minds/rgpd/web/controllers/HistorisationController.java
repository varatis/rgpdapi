package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.HistorisationCreationDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.services.HistorisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Traçabilité du registre de traitements.
 * <ul>
 *   <li>RG1 : l'historique d'un traitement est consultable ;</li>
 *   <li>CA4 : l'utilisateur peut y ajouter lui-même une entrée.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Historisation Controller", description = "Historique des modifications du registre et des traitements")
public class HistorisationController {

    private final HistorisationService historisationService;

    @GetMapping("/traitements/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historique des modifications d'un traitement (RG1)")
    public ResponseEntity<List<HistorisationDTO>> getHistoriqueTraitement(@PathVariable String id) {
        return ResponseEntity.ok(historisationService.getHistoriqueTraitement(Integer.parseInt(id)));
    }

    @PostMapping("/traitements/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ajoute une entrée d'historique saisie par l'utilisateur (CA4)")
    public ResponseEntity<HistorisationDTO> ajouterHistoriqueTraitement(
            @PathVariable String id,
            @Valid @RequestBody HistorisationCreationDTO creation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historisationService.ajouterHistoriqueTraitement(Integer.parseInt(id), creation));
    }

    @GetMapping("/registre/historique")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Historique du registre d'un client (imports, suppressions, versions)")
    public ResponseEntity<List<HistorisationDTO>> getHistoriqueRegistre(
            @RequestParam @Size(max = 255) String clientNom) {
        return ResponseEntity.ok(historisationService.getHistoriqueRegistre(clientNom));
    }

    @PostMapping("/registre/historique")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ajoute une entrée d'historique sur le registre (CA4)")
    public ResponseEntity<HistorisationDTO> ajouterHistoriqueRegistre(
            @RequestParam @Size(max = 255) String clientNom,
            @Valid @RequestBody HistorisationCreationDTO creation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(historisationService.ajouterHistoriqueRegistre(clientNom, creation));
    }
}
