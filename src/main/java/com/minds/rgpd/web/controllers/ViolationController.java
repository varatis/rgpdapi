package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.ViolationDTO;
import com.minds.rgpd.business.dtos.ViolationFilterCriteria;
import com.minds.rgpd.business.dtos.ViolationPartielDTO;
import com.minds.rgpd.business.enums.ViolationStatut;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/violations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Violation Controller", description = "Gère le registre des violations de données à caractère personnel")
public class ViolationController {

    private final ViolationService violationService;

    @GetMapping
    @Operation(summary = "Liste paginée des violations d'un client")
    public ResponseEntity<Page<ViolationPartielDTO>> getViolations(
            @PageableDefault(size = 20, sort = "dateViolation", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @Size(max = 255) String clientNom,
            @RequestParam(required = false) @Size(max = 255) String natureViolation,
            @RequestParam(required = false) @Size(max = 255) String donneesConcernees,
            @RequestParam(required = false) Boolean risqueEleveDroitsLibertes,
            @RequestParam(required = false) ViolationStatut statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateViolationDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateViolationFin,
            @RequestParam(required = false) @PositiveOrZero Integer nombrePersonnesConcerneesMin,
            @RequestParam(required = false) @PositiveOrZero Integer nombrePersonnesConcerneesMax) {
        ViolationFilterCriteria criteria = new ViolationFilterCriteria(
                natureViolation, donneesConcernees, risqueEleveDroitsLibertes, statut,
                dateViolationDebut, dateViolationFin,
                nombrePersonnesConcerneesMin, nombrePersonnesConcerneesMax);
        return ResponseEntity.ok(violationService.getViolations(pageable, clientNom, criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une violation")
    public ResponseEntity<ViolationDTO> getViolation(@PathVariable UUID id) {
        return ResponseEntity.ok(violationService.getOneViolation(id));
    }

    @PostMapping
    @Operation(summary = "Enregistre une nouvelle violation")
    public ResponseEntity<ViolationDTO> postViolation(@RequestBody @Valid ViolationDTO violation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(violationService.createViolation(violation));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une violation identifiée par son UUID")
    public ResponseEntity<ViolationDTO> putViolation(@PathVariable UUID id, @RequestBody @Valid ViolationDTO violation) {
        return ResponseEntity.ok(violationService.updateViolation(id, violation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprime une violation en l'identifiant avec son UUID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            violationService.deleteViolationById(id);
            return ResponseEntity.noContent().build();
        }
        catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
