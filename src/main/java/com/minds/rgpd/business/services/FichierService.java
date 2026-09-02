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

    InfoFichierDTOBuilder importFichier(MultipartFile fichier, InfoFichierDTOBuilder infoFichier,
                                        boolean confirmerRemplacement) throws IOException;

    ImportApercuDTO apercuImport(String nomFichier);

    byte[] generationExcelRegistreTraitements(ClientDTO client, String fileName) throws IOException;
}
