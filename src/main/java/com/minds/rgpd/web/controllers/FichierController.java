package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ImportApercuDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import com.minds.rgpd.business.services.ClientService;
import com.minds.rgpd.business.services.FichierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    @Value("${application.fichier.upload.dir}")
    private String uploadDir;

    @GetMapping("/apercu")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Conséquences d'un import du fichier, sans l'exécuter (RG3)")
    public ResponseEntity<ImportApercuDTO> apercuImport(@RequestParam("nomFichier") String nomFichier) {
        return ResponseEntity.ok(fichierService.apercuImport(nomFichier));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Importe un registre RGPD, en remplaçant les données existantes du client")
    public ResponseEntity<InfoFichierDTO> importFichierRgpd(
            @RequestParam("file") MultipartFile fichier,
            @RequestParam(value = "confirmerRemplacement", defaultValue = "false") boolean confirmerRemplacement) {
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
            infoFichier = fichierService.importFichier(fichier, infoFichier, confirmerRemplacement);

            if (!infoFichier.build().confirmationRequise()) {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                Path filePath = uploadPath.resolve(Paths.get(originalFilename));
                Files.copy(fichier.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            return ResponseEntity.status(500).body(infoFichier.statusFichier(e.getMessage()).build());
        }

        InfoFichierDTO resultat = infoFichier.build();
        if (resultat.confirmationRequise()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resultat);
        }
        return ResponseEntity.ok(resultat);
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Exporte le registre de traitements du client au format d'import")
    public ResponseEntity<Resource> exportExcel(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "clientNom", required = false) String clientNomParam) throws IOException {

        String clientName = resolveClientName(jwt, clientNomParam);
        ClientDTO client = clientService.getClientByNom(clientName);

        String version = Objects.toString(client.version(), "1.0");
        String fileName = "%s_CREATIVE_Registre RGPD_ed%s".formatted(client.nom(), version);

        Resource resource = new ByteArrayResource(fichierService.generationExcelRegistreTraitements(client, fileName));

        return ResponseEntity.ok().header( HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + fileName + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }

    private String resolveClientName(Jwt jwt, String clientNomParam) {
        String claim = Objects.isNull(jwt) ? null : jwt.getClaimAsString("client_groups");
        if (Objects.nonNull(claim) && !claim.isBlank()) {
            String valeur = claim.trim();
            if (valeur.startsWith("[") && valeur.endsWith("]")) {
                valeur = valeur.substring(1, valeur.length() - 1);
            }
            return valeur.split(",")[0].trim();
        }
        if (Objects.nonNull(clientNomParam) && !clientNomParam.isBlank()) {
            return clientNomParam;
        }
        throw new IllegalArgumentException("Aucun client associé à l'utilisateur connecté");
    }
}
