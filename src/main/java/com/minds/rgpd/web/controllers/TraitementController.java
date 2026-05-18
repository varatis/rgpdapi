package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.services.TraitementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/traitements")
@RequiredArgsConstructor
@Tag(name = "Traitement Controller", description = "Gère les entités Traitements")
public class TraitementController {

    private final TraitementService traitementService;

    @GetMapping
    public ResponseEntity<List<TraitementDTO>> getTraitements() {
        List<TraitementDTO> traitements = traitementService.getTraitements();
        return ResponseEntity.ok(traitements);
    }
}
