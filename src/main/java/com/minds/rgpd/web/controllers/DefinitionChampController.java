package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.DefinitionChampDTO;
import com.minds.rgpd.business.services.DefinitionChampService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/definitions-champs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Définition Champ Controller", description = "Définitions métier des champs du registre, extraites de l'onglet FR_Définitions à l'import")
public class DefinitionChampController {

    private final DefinitionChampService definitionChampService;

    @GetMapping
    public ResponseEntity<List<DefinitionChampDTO>> getDefinitions(
            @RequestParam @Size(max = 255) String clientNom,
            @RequestParam(required = false) @Size(max = 20) String edition) {
        return ResponseEntity.ok(definitionChampService.getDefinitions(clientNom, edition));
    }
}
