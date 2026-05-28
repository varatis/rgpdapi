package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.FichierService;
import com.minds.rgpd.business.utilities.StatusFichierEnum;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/importFichierRgpd")
@RequiredArgsConstructor
@Tag(name="Fichier Controller", description = "Gère l'import du fichier RGPD")
public class FichierController {

    @Value("${application.fichier.upload.dir}")
    private String uploadDir;

    private final FichierService fichierService;

    @PostMapping
    public ResponseEntity<InfoFichierDTO> importFichierRgpd(@RequestParam("file") MultipartFile fichier) {
        LocalDateTime dateReception = LocalDateTime.now();
        String originalFilename = fichier.getOriginalFilename();
        assert Objects.nonNull(originalFilename);
        InfoFichierDTOBuilder infoFichier = InfoFichierDTO
                .builder()
                .nomFichier(originalFilename)
                .dateReception(dateReception)
                .statusFichier(StatusFichierEnum.KO);
        if(fichier.isEmpty()) {
            return ResponseEntity.badRequest().body(infoFichier.build());
        }
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(Paths.get(originalFilename));
            Files.copy(fichier.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            infoFichier = fichierService.importFichier(fichier, infoFichier);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(infoFichier.build());
        }

        return ResponseEntity.ok(infoFichier.build());
    }

}
