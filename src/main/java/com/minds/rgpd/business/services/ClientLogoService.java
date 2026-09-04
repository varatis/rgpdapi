package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ClientLogoContentDTO;
import com.minds.rgpd.business.dtos.ClientLogoInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface ClientLogoService {

    /** Crée ou remplace le logo du client. */
    ClientLogoInfoDTO updateLogo(UUID clientId, MultipartFile fichier);

    /** Contenu binaire du logo, pour la réponse HTTP. */
    ClientLogoContentDTO getLogo(UUID clientId);

    /** Métadonnées seules (nom, taille, type) : ne lit pas le contenu. */
    ClientLogoInfoDTO getLogoInfo(UUID clientId);

    /** ETag seul, pour traiter une requête conditionnelle sans lire le contenu. */
    Optional<String> getEtag(UUID clientId);

    void deleteLogo(UUID clientId);
}
