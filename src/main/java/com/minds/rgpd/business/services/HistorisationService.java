package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.HistorisationCreationDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;

import java.util.List;

public interface HistorisationService {

    String MOTIF_IMPORT = "Import du registre";

    List<HistorisationDTO> getHistoriqueTraitement(int idFonctionnel);

    List<HistorisationDTO> getHistoriqueRegistre(String clientNom);

    HistorisationDTO ajouterHistoriqueTraitement(int idFonctionnel, HistorisationCreationDTO creation);

    HistorisationDTO ajouterHistoriqueRegistre(String clientNom, HistorisationCreationDTO creation);

    void historiserTraitement(Traitement traitement, String motif);

    void historiserRegistre(Client client, String motif);
}
