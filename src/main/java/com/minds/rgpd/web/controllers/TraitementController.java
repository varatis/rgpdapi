package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.services.TraitementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/traitements")
@RequiredArgsConstructor
@Tag(name = "Traitement Controller", description = "Gère les entités Traitements")
public class TraitementController {

    private final TraitementService traitementService;

    @GetMapping
    public ResponseEntity<Page<TraitementPartielDTO>> getTraitements(@PageableDefault() Pageable pageable) {
        Page<TraitementPartielDTO> traitements = traitementService.getTraitements(pageable);
        return ResponseEntity.ok(traitements);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TraitementDTO> getTraitement(@PathVariable Integer id) {
        TraitementDTO traitement = traitementService.getOneTraitement(id);
        return ResponseEntity.ok(traitement);
    }
}
