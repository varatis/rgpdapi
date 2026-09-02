package com.minds.rgpd.business.services;

import com.minds.rgpd.business.dtos.HistorisationCreationDTO;
import com.minds.rgpd.business.dtos.HistorisationDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;

import java.util.List;

/**
 * Traçabilité du registre de traitements.
 * <ul>
 *   <li>RG1 : toute modification d'un traitement est historisée ;</li>
 *   <li>RG2/RG3 : un import est historisé au niveau du registre du client ;</li>
 *   <li>CA4 : l'utilisateur peut lui-même ajouter une entrée d'historique.</li>
 * </ul>
 */
public interface HistorisationService {

    /** Motif enregistré automatiquement lors du remplacement du registre par un import. */
    String MOTIF_IMPORT = "Import du registre";

    /** Historique d'un traitement, du plus récent au plus ancien. */
    List<HistorisationDTO> getHistoriqueTraitement(int idFonctionnel);

    /** Historique du registre d'un client, du plus récent au plus ancien. */
    List<HistorisationDTO> getHistoriqueRegistre(String clientNom);

    /** Ajout manuel d'une entrée d'historique sur un traitement (CA4). */
    HistorisationDTO ajouterHistoriqueTraitement(int idFonctionnel, HistorisationCreationDTO creation);

    /** Ajout manuel d'une entrée d'historique sur le registre d'un client. */
    HistorisationDTO ajouterHistoriqueRegistre(String clientNom, HistorisationCreationDTO creation);

    /**
     * Historisation automatique d'une modification de traitement (RG1).
     * Appelée par les services métier, dans la transaction courante.
     */
    void historiserTraitement(Traitement traitement, String motif);

    /** Historisation automatique d'un évènement de registre (import, changement de version). */
    void historiserRegistre(Client client, String motif);
}
