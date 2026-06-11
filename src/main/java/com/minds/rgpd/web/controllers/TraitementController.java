package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.minds.rgpd.business.services.TraitementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.exception.DataException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    public ResponseEntity<TraitementDTO> getTraitement(@PathVariable String id) {
        TraitementDTO traitement = traitementService.getOneTraitement(Integer.parseInt(id));
        return ResponseEntity.ok(traitement);
    }

    @PostMapping
    public ResponseEntity<TraitementDTO> postTraitement(@RequestBody TraitementDTO traitement) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(traitementService.createTraitement(traitement));
        } catch (DataException e) {
           return ResponseEntity.status(HttpStatus.CONFLICT).body(traitement);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(traitement);
        }
    }

    @GetMapping("/nextId")
    public ResponseEntity<Integer> getNextIdFonctionnel(/*UUID idClient*/){
        return ResponseEntity.ok(traitementService.getNextIdFonctionnel());
    }
}
