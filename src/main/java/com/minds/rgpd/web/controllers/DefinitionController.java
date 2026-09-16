package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.services.DefinitionService;
import com.minds.rgpd.business.utilities.AccessUserInformation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/definitions")
@RequiredArgsConstructor
@Tag(name = "Definition Controller", description = "Référentiels proposés en liste dans le formulaire d'un traitement")
public class DefinitionController {

    private final DefinitionService definitionService;

    @GetMapping("/{type}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Valeurs d'un référentiel (sensibilite, etude-impact, liceite-traitement) pour le client de l'utilisateur connecté")
    public ResponseEntity<List<String>> getValues(@AuthenticationPrincipal Jwt jwt, @PathVariable String type) {
        String clientNom = AccessUserInformation.getClientUniqueDuJeton(jwt);
        return ResponseEntity.ok(definitionService.getValues(clientNom, type));
    }
}
