package com.minds.rgpd.web.controllers;


import com.minds.rgpd.business.dtos.ProfilDTO;
import com.minds.rgpd.business.services.ProfilService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/profils")
@RequiredArgsConstructor
@Tag(name = "Profil Controller", description = "Gère les entités Profil")
public class ProfilController {

    private final ProfilService profilService;

    @GetMapping
    public ResponseEntity<List<ProfilDTO>> getProfils() {
        List<ProfilDTO> profils = profilService.getProfils();
        return ResponseEntity.ok(profils);
    }
}
