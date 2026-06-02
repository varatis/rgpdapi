package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RowFileToTraitement {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    public static TraitementDTO map(List<String> cellules, List<EtablissementDTO> etablissements, ClientDTO client, Integer version) {
        Integer id = (int) Double.parseDouble(cellules.get(0));
        String nomTraitement = cellules.get(3);
        LocalDate dateIdentificationTraitement = null;
        if (!cellules.get(4).isBlank()) {
            dateIdentificationTraitement = LocalDate.parse(cellules.get(4), formatter);
        }

        // Mécanique pour récupérer soit la date (si champ date) ou dernière date (si champ texte)
        LocalDate dateMiseAJour = null;
        if (!cellules.get(5).isBlank()) {
            String substring = StringUtils.substring(cellules.get(5), cellules.get(5).length() - 10, cellules.get(5).length());
            if (substring.matches("\\d{2}/\\d{2}/\\d{4}")) {
                dateMiseAJour = LocalDate.parse(substring, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } else {
                dateMiseAJour = LocalDate.parse(cellules.get(5), formatter);
            }
        }

        String historiqueModifications = cellules.get(6);
        String dataProtectionOfficer = cellules.get(7);
        String responsableTraitement = cellules.get(8);
        String gestionnaireMiseEnOeuvre = cellules.get(9);
        String finalitePrincipale = cellules.get(10);
        String sousFinalites = cellules.get(11);
        String categoriesPersonnesConcernees = cellules.get(12);
        String donneesIdentification = cellules.get(13);
        String donneesConnexion = cellules.get(14);
        String donneesLocalisation = cellules.get(15);
        String donneesComportementEtViePersonnelle = cellules.get(16);
        String donneesEconomiquesEtFinancieres = cellules.get(17);
        String donneesProfessionnelles = cellules.get(18);
        String categoriesParticulieres = cellules.get(19);
        String sensibilite = cellules.get(20);
        String etudeImpact = cellules.get(21);
        String canauxDeCollecteDeDonnees = cellules.get(22);
        String liceiteDuTraitement = cellules.get(23);
        String recoursAuTraitement = cellules.get(24);
        String emplacementPhysiqueTraitement = cellules.get(25);
        String dispositionSecuriteDonneesPhysiques = cellules.get(26);
        String emplacementNumerique = cellules.get(27);
        String dispositionSecuriteDonneesNumeriques = cellules.get(28);
        String hebergement = cellules.get(29);
        String dureeDeConservation = cellules.get(30);
        String archivage = cellules.get(31);
        String dureeArchivage = cellules.get(32);
        String categoriesDestinataires = cellules.get(33);
        String raisonTransfertVersCatDestinataire = cellules.get(34);
        String transfertHorsUE = cellules.get(35);
        String paysDestinataire = cellules.get(36);
        String commentaires = cellules.get(37);

        return new TraitementDTO(
                id, nomTraitement, etablissements, gestionnaireMiseEnOeuvre, finalitePrincipale, client, version,
                dateIdentificationTraitement, dateMiseAJour, historiqueModifications,
                dataProtectionOfficer, responsableTraitement, gestionnaireMiseEnOeuvre,
                sousFinalites, categoriesPersonnesConcernees, donneesIdentification,
                donneesConnexion, donneesLocalisation, donneesComportementEtViePersonnelle,
                donneesEconomiquesEtFinancieres, donneesProfessionnelles, categoriesParticulieres,
                sensibilite, etudeImpact, canauxDeCollecteDeDonnees, liceiteDuTraitement, Boolean.parseBoolean(recoursAuTraitement),
                emplacementPhysiqueTraitement, dispositionSecuriteDonneesPhysiques, emplacementNumerique, dispositionSecuriteDonneesNumeriques, hebergement,
                dureeDeConservation, Boolean.parseBoolean(archivage), dureeArchivage, categoriesDestinataires,
                raisonTransfertVersCatDestinataire, Boolean.parseBoolean(transfertHorsUE), paysDestinataire, commentaires);
    }
}
