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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    /**
     * Renvoie les octets du logo.
     * <p>
     * L'endpoint exige un JWT comme le reste de l'API : une balise
     * {@code <img src="...">} ne peut pas porter d'en-tête Authorization, le
     * front doit donc récupérer le logo en {@code responseType: 'blob'} via son
     * intercepteur, puis passer par {@code URL.createObjectURL(blob)}.
     */
    @GetMapping("/{id}/logo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logo du client (image binaire)")
    public ResponseEntity<byte[]> getLogo(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {

        // L'ETag est lu seul : une requête conditionnelle se résout en 304 sans
        // que le contenu binaire ne soit chargé depuis la base.
        String etag = clientLogoService.getEtag(id)
                .orElseThrow(() -> new ResourceNotFoundException("Logo", "client", id));
        String enteteEtag = "\"%s\"".formatted(etag);

        if (enteteEtag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(enteteEtag).build();
        }

        ClientLogoContentDTO logo = clientLogoService.getLogo(id);
        return ResponseEntity.ok()
                .eTag(enteteEtag)
                // Positionner Cache-Control ici désactive le "no-store" que Spring
                // Security applique par défaut : son writer laisse l'en-tête en place
                // dès lors qu'il est déjà présent sur la réponse.
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .contentType(MediaType.parseMediaType(logo.contentType()))
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
