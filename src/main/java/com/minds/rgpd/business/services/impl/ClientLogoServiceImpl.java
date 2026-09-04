package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.dtos.ClientLogoContentDTO;
import com.minds.rgpd.business.dtos.ClientLogoInfoDTO;
import com.minds.rgpd.business.exceptions.InvalidFileException;
import com.minds.rgpd.business.exceptions.ResourceNotFoundException;
import com.minds.rgpd.business.services.ClientLogoService;
import com.minds.rgpd.business.utilities.FormatImage;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.ClientLogo;
import com.minds.rgpd.persistence.repositories.ClientLogoRepository;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClientLogoServiceImpl implements ClientLogoService {

    /** Le nom de fichier n'est qu'une donnée d'affichage : on le tronque au format de la colonne. */
    private static final int LONGUEUR_MAX_NOM_FICHIER = 255;

    private final ClientRepository clientRepository;
    private final ClientLogoRepository clientLogoRepository;

    /**
     * Deuxième garde-fou uniquement : la vraie protection est
     * {@code spring.servlet.multipart.max-file-size}, appliquée par le conteneur
     * pendant la lecture du flux, avant que ce service ne soit atteint. Ce
     * contrôle-ci n'existe que pour produire un message d'erreur exploitable.
     */
    @Value("${application.logo.taille-max:1MB}")
    private DataSize tailleMax;

    @Override
    @Transactional
    public ClientLogoInfoDTO updateLogo(UUID clientId, MultipartFile fichier) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", clientId));

        byte[] contenu = lireContenu(fichier);
        if (contenu.length == 0) {
            throw new InvalidFileException("Le fichier envoyé est vide.");
        }
        if (contenu.length > tailleMax.toBytes()) {
            throw new InvalidFileException(
                    "Le logo ne doit pas dépasser %d Ko.".formatted(tailleMax.toKilobytes()));
        }

        String contentType = FormatImage.detectContentType(contenu)
                .orElseThrow(() -> new InvalidFileException(
                        "Format de logo non accepté. Formats acceptés : PNG, JPEG, WebP."));

        // L'identifiant est dérivé de l'association par @MapsId : le renseigner à la
        // main produit une entité détachée que Spring Data confie à merge(), lequel
        // échoue à recopier l'association avant que l'id ne soit résolu
        // (AssertionFailure: null identifier). Le logo existant est donc chargé pour
        // être mis à jour, et une instance neuve — dont Hibernate dérivera l'id au
        // persist — est créée à défaut. Le même appel couvre les deux cas.
        ClientLogo logo = clientLogoRepository.findById(clientId)
                .orElseGet(() -> ClientLogo.builder().client(client).build());

        logo.setContent(contenu);
        logo.setContentType(contentType);
        logo.setEtag(empreinte(contenu));
        logo.setFileName(nomFichierAffichable(fichier));
        logo.setFileSize(contenu.length);

        ClientLogo sauvegarde = clientLogoRepository.save(logo);
        log.info("Logo du client {} mis à jour : {} ({} octets, {})",
                clientId, sauvegarde.getFileName(), sauvegarde.getFileSize(), sauvegarde.getContentType());

        return toInfoDTO(sauvegarde);
    }

    @Override
    public ClientLogoContentDTO getLogo(UUID clientId) {
        ClientLogo logo = clientLogoRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Logo", "client", clientId));
        return new ClientLogoContentDTO(logo.getContent(), logo.getContentType(), logo.getEtag());
    }

    @Override
    public ClientLogoInfoDTO getLogoInfo(UUID clientId) {
        return clientLogoRepository.findProjectedByClientId(clientId)
                .map(info -> ClientLogoInfoDTO.builder()
                        .nomFichier(info.getFileName())
                        .taille(info.getFileSize())
                        .contentType(info.getContentType())
                        .etag(info.getEtag())
                        .build())
                .orElseThrow(() -> new ResourceNotFoundException("Logo", "client", clientId));
    }

    @Override
    public Optional<String> getEtag(UUID clientId) {
        return clientLogoRepository.findEtagByClientId(clientId)
                .map(ClientLogoRepository.ClientLogoEtag::getEtag);
    }

    @Override
    @Transactional
    public void deleteLogo(UUID clientId) {
        if (!clientLogoRepository.existsByClientId(clientId)) {
            throw new ResourceNotFoundException("Logo", "client", clientId);
        }
        clientLogoRepository.deleteById(clientId);
        log.info("Logo du client {} supprimé", clientId);
    }

    private byte[] lireContenu(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            log.error("Lecture du logo impossible : {}", e.getMessage(), e);
            throw new InvalidFileException("Lecture du fichier impossible : %s".formatted(e.getMessage()));
        }
    }

    /**
     * SHA-256 du contenu, utilisé comme ETag : un logo réenvoyé à l'identique
     * conserve son empreinte et n'invalide donc pas les caches navigateur.
     */
    private String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est requis par la spécification de la plateforme Java.
            throw new IllegalStateException("Algorithme SHA-256 indisponible", e);
        }
    }

    /**
     * Le nom fourni par le navigateur est arbitraire (longueur, caractères de
     * contrôle, séquences de traversée de chemin). Il n'est conservé que pour
     * l'affichage : on se contente donc de garantir qu'il tient en base.
     */
    private String nomFichierAffichable(MultipartFile fichier) {
        String nom = fichier.getOriginalFilename();
        if (nom == null || nom.isBlank()) {
            return "logo";
        }
        return nom.length() > LONGUEUR_MAX_NOM_FICHIER ? nom.substring(0, LONGUEUR_MAX_NOM_FICHIER) : nom;
    }

    private ClientLogoInfoDTO toInfoDTO(ClientLogo logo) {
        return ClientLogoInfoDTO.builder()
                .nomFichier(logo.getFileName())
                .taille(logo.getFileSize())
                .contentType(logo.getContentType())
                .etag(logo.getEtag())
                .build();
    }
}
