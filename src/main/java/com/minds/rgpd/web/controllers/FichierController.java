package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.ClientService;
import com.minds.rgpd.business.services.EtablissementService;
import com.minds.rgpd.business.services.FichierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
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
@Tag(name = "Fichier Controller", description = "Gère l'import du fichier RGPD")
public class FichierController {

    private final ClientService clientService;
    private final FichierService fichierService;
    private final EtablissementService etablissementService;
    @Value("${application.fichier.upload.dir}")
    private String uploadDir;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InfoFichierDTO> importFichierRgpd(@RequestParam("file") MultipartFile fichier) {
        LocalDateTime dateReception = LocalDateTime.now();
        String originalFilename = fichier.getOriginalFilename();
        assert Objects.nonNull(originalFilename);
        InfoFichierDTOBuilder infoFichier = InfoFichierDTO
                .builder()
                .nomFichier(originalFilename)
                .dateReception(dateReception)
                .statusFichier("KO");
        if (fichier.isEmpty()) {
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
            return ResponseEntity.status(500).body(infoFichier.statusFichier(e.getMessage()).build());
        }

        return ResponseEntity.ok(infoFichier.build());
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportExcel(@AuthenticationPrincipal Jwt jwt) throws IOException {

        //Récupération du client concerné
        String clientName = jwt.getClaimAsString("client_groups").substring(1, jwt.getClaimAsString("client_groups").length() - 1);
        ClientDTO client = clientService.getClientByNom(clientName);

        //Récupération du nom de l'établissement concerné
        //EtablissementDTO ets = etablissementService.

        String etsName = "EtsNameTest";    //TODO - A récupérer

        //Récupération du département concerné
        String departmentName = "depNameTest";    //TODO - A récupérer

        String fileName = /*ets.nom() + */ etsName + "_" + departmentName + "_CREATIVE_Registre RGPD_ed" + client.version();
        Resource resource = new ByteArrayResource(fichierService.generationExcelRegistreTraitements(client, fileName));

        return ResponseEntity.ok().header( HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + fileName + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }
}
