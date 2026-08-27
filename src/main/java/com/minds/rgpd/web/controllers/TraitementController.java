package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementFilterCriteria;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.TraitementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.service.GenericResponseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/traitements")
@RequiredArgsConstructor
@Validated
@Tag(name = "Traitement Controller", description = "Gère les entités Traitements")
public class TraitementController {

    private final TraitementService traitementService;

    @GetMapping
    public ResponseEntity<Page<TraitementPartielDTO>> getTraitements(
            @PageableDefault(size = 20, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) @Size(max = 255) String clientNom,
            @RequestParam(required = false) @Size(max = 255) String nom,
            @RequestParam(required = false) @Size(max = 255) String gestionnaireMiseEnOeuvre,
            @RequestParam(required = false) @Size(max = 255) String finalitePrincipale) {
        TraitementFilterCriteria criteria = new TraitementFilterCriteria(nom, gestionnaireMiseEnOeuvre, finalitePrincipale);
        Page<TraitementPartielDTO> traitements = traitementService.getTraitements(pageable, clientNom, criteria);
        return ResponseEntity.ok(traitements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TraitementDTO> getTraitement(@PathVariable String id) {
        TraitementDTO traitement = traitementService.getOneTraitement(Integer.parseInt(id));
        return ResponseEntity.ok(traitement);
    }

    @PostMapping
    public ResponseEntity<TraitementDTO> postTraitement(@RequestBody TraitementDTO traitement) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(traitementService.createTraitement(traitement));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TraitementDTO> putTraitement(@PathVariable String id, @RequestBody TraitementDTO traitement) {
        return ResponseEntity.ok(traitementService.updateTraitement(Integer.parseInt(id), traitement));
    }

    @GetMapping("/{id}/historique")
    @Operation(summary = "RG1 - Historique des modifications d'un traitement (date + motif), du plus récent au plus ancien")
    public ResponseEntity<List<HistorisationDTO>> getHistoriqueTraitement(@PathVariable String id) {
        return ResponseEntity.ok(traitementService.getHistoriqueTraitement(Integer.parseInt(id)));
    }

    @GetMapping("/nextId")
    public ResponseEntity<Integer> getNextIdFonctionnel(){
        return ResponseEntity.ok(traitementService.getNextIdFonctionnel());
    }

    @DeleteMapping("/duplicates")
    public ResponseEntity<Integer> deleteDuplicateTraitements() {
        Integer deletedCount = traitementService.deleteDuplicateTraitements();
        return ResponseEntity.ok(deletedCount);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprime un traitement en l'identifiant avec son UUID")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        try {
            traitementService.deleteTraitementById(id);
            return ResponseEntity.noContent().build();
        }
        catch (ResourceNotFoundException ex){
            return ResponseEntity.notFound().build();
        }
    }
}
