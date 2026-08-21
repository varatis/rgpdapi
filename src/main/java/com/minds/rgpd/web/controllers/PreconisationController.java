package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationFilterCriteria;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.minds.rgpd.business.services.PreconisationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/preconisations")
@RequiredArgsConstructor
@Validated
@Tag(name = "Préconisation Controller", description = "Consultation du suivi des préconisations associées aux traitements")
public class PreconisationController {

    private final PreconisationService preconisationService;

    @GetMapping
    @Operation(summary = "Liste paginée des préconisations d'un client")
    public ResponseEntity<Page<PreconisationPartielDTO>> getPreconisations(
            @PageableDefault(size = 20, sort = "libelle", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false) @Size(max = 255) String clientNom,
            @RequestParam(required = false) @Size(max = 255) String libelle,
            @RequestParam(required = false) @Size(max = 100) String etatAvancement,
            @RequestParam(required = false) UUID idTraitement
    ) {
        PreconisationFilterCriteria criteria = new PreconisationFilterCriteria(libelle, etatAvancement, idTraitement);
        return ResponseEntity.ok(preconisationService.getPreconisations(pageable, clientNom, criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une préconisation, y compris son état d'avancement")
    public ResponseEntity<PreconisationDTO> getPreconisation(@PathVariable UUID id) {
        return ResponseEntity.ok(preconisationService.getOnePreconisation(id));
    }
}
