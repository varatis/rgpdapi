package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.minds.rgpd.business.services.UtilisateurService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateur Controller", description = "Gère les entités Utilisateur")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UtilisateurDTO>> getUtilisateurs() {
        List<UtilisateurDTO> utilisateurs = utilisateurService.getUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }
}
