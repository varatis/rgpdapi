package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ClientWriteDTO;
import com.minds.rgpd.business.dtos.ClientLogoContentDTO;
import com.minds.rgpd.business.dtos.ClientLogoInfoDTO;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.ClientLogoService;
import com.minds.rgpd.business.services.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Client Controller", description = "Gère les entités Client")
public class ClientController {

    private final ClientService clientService;
    private final ClientLogoService clientLogoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClientDTO>> getClients() {
        List<ClientDTO> clients = clientService.getClients();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/nom/{nom}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClientDTO> getClientByNom(@PathVariable String nom) {
        ClientDTO client = clientService.getClientByNom(nom);
        return ResponseEntity.ok(client);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Crée un client")
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientWriteDTO payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(payload));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Modifie les informations d'un client")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable UUID id,
            @Valid @RequestBody ClientWriteDTO payload) {
        return ResponseEntity.ok(clientService.updateClient(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Supprime un client et ses utilisateurs Keycloak")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    /** Renvoie les octets du logo. */
    @GetMapping("/{id}/logo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logo du client (image binaire)")
    public ResponseEntity<byte[]> getLogo(
            @PathVariable UUID id,
            @RequestHeader(value = jakarta.servlet.http.HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        String etag = clientLogoService.getEtag(id)
                .orElseThrow(() -> new ResourceNotFoundException("Logo", "client", id));
        String enteteEtag = "\"%s\"".formatted(etag);

        if (enteteEtag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(enteteEtag).build();
        }

        ClientLogoContentDTO logo = clientLogoService.getLogo(id);
        return ResponseEntity.ok()
                .eTag(enteteEtag)
                .cacheControl(org.springframework.http.CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .contentType(javax.media.MediaType.parseMediaType(logo.contentType()))
                .body(logo.content());
    }

    /** Métadonnées du logo pour l'écran de modification : ne transfère pas l'image. */
    @GetMapping("/{id}/logo/info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Nom, taille et type du logo, sans le contenu")
    public ResponseEntity<ClientLogoInfoDTO> getLogoInfo(@PathVariable UUID id) {
        return ResponseEntity.ok(clientLogoService.getLogoInfo(id));
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Dépose ou remplace le logo du client (PNG, JPEG ou WebP)")
    public ResponseEntity<ClientLogoInfoDTO> updateLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile fichier) {
        return ResponseEntity.ok(clientLogoService.updateLogo(id, fichier));
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('SUPERADMIN')")
    @Operation(summary = "Supprime le logo du client")
    public ResponseEntity<Void> deleteLogo(@PathVariable UUID id) {
        clientLogoService.deleteLogo(id);
        return ResponseEntity.noContent().build();
    }
}