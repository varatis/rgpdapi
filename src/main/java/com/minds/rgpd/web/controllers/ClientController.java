package com.minds.rgpd.web.controllers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.services.ClientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Client Controller", description = "Gère les entités Client")
public class ClientController {

    private final ClientService clientService;

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
}
