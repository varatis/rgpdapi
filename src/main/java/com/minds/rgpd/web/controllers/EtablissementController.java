package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.services.EtablissementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/etablissements")
@RequiredArgsConstructor
@Tag(name = "Etablissement Controller", description = "Gère les entités Etablissement")
public class EtablissementController {

    private final EtablissementService etablissementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EtablissementDTO>> getEtablissements() {
        List<EtablissementDTO> etablissements = etablissementService.getEtablissements();
        return ResponseEntity.ok(etablissements);
    }
}
