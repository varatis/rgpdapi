package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.ImportApercuDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO.InfoFichierDTOBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public interface FichierService {

    InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier) throws IOException;

    /**
     * Import du registre avec gestion du remplacement (RG2/RG3).
     *
     * @param confirmerRemplacement vrai lorsque l'utilisateur a confirmé, dans la
     *                              modale d'avertissement, le remplacement de la
     *                              totalité de ses données par celles du fichier.
     */
    InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier,
                                        boolean confirmerRemplacement) throws IOException;

    /**
     * Conséquences d'un import du fichier nommé {@code nomFichier}, sans rien
     * modifier (RG3) : alimente la modale d'avertissement de l'interface.
     */
    ImportApercuDTO apercuImport(String nomFichier);

    byte[] generationExcelRegistreTraitements(ClientDTO client, String fileName) throws IOException;
}
