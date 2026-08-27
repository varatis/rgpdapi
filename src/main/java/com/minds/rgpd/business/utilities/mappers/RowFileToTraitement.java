package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.DefinitionDTO;
import com.minds.rgpd.business.dtos.DureeDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.ResponsableTraitementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.EtudeImpact;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.LiceiteTraitement;
import com.minds.rgpd.persistence.entities.Sensibilite;
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
        String donneesConcernees = cellules.get(2);
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
        ResponsableTraitementDTO responsableTraitement = responsable(cellules.get(8));
        String gestionnaireMiseEnOeuvre = cellules.get(9);
        DefinitionDTO finalitePrincipale = definition(FinalitePrincipale.TYPE, cellules.get(10));
        String sousFinalites = cellules.get(11);
        String categoriesPersonnesConcernees = cellules.get(12);
        String donneesIdentification = cellules.get(13);
        String donneesConnexion = cellules.get(14);
        String donneesLocalisation = cellules.get(15);
        String donneesComportementEtViePersonnelle = cellules.get(16);
        String donneesEconomiquesEtFinancieres = cellules.get(17);
        String donneesProfessionnelles = cellules.get(18);
        String categoriesParticulieres = cellules.get(19);
        DefinitionDTO sensibilite = definition(Sensibilite.TYPE, cellules.get(20));
        DefinitionDTO etudeImpact = definition(EtudeImpact.TYPE, cellules.get(21));
        String canauxDeCollecteDeDonnees = cellules.get(22);
        DefinitionDTO liceiteDuTraitement = definition(LiceiteTraitement.TYPE, cellules.get(23));
        String recoursAuTraitement = cellules.get(24);
        String emplacementPhysiqueTraitement = cellules.get(25);
        String dispositionSecuriteDonneesPhysiques = cellules.get(26);
        String emplacementNumerique = cellules.get(27);
        String dispositionSecuriteDonneesNumeriques = cellules.get(28);
        String hebergement = cellules.get(29);
        DureeDTO dureeDeConservation = duree(Duree.CONSERVATION, cellules.get(30));
        String archivage = cellules.get(31);
        DureeDTO dureeArchivage = duree(Duree.ARCHIVAGE, cellules.get(32));
        String categoriesDestinataires = cellules.get(33);
        String raisonTransfertVersCatDestinataire = cellules.get(34);
        String transfertHorsUE = cellules.get(35);
        String paysDestinataire = cellules.get(36);
        String commentaires = cellules.get(37);

        return new TraitementDTO(
                null,id, nomTraitement, donneesConcernees, etablissements, finalitePrincipale, client, version,
                dateIdentificationTraitement, dateMiseAJour, historiqueModifications,
                null, // motifModification : renseigné uniquement à la modification via l'IHM (RG1/CA4)
                dataProtectionOfficer, responsableTraitement, gestionnaireMiseEnOeuvre,
                sousFinalites, categoriesPersonnesConcernees, donneesIdentification,
                donneesConnexion, donneesLocalisation, donneesComportementEtViePersonnelle,
                donneesEconomiquesEtFinancieres, donneesProfessionnelles, categoriesParticulieres,
                sensibilite, etudeImpact, canauxDeCollecteDeDonnees, liceiteDuTraitement, Boolean.parseBoolean(recoursAuTraitement),
                emplacementPhysiqueTraitement, dispositionSecuriteDonneesPhysiques, emplacementNumerique, dispositionSecuriteDonneesNumeriques, hebergement,
                dureeDeConservation, Boolean.parseBoolean(archivage), dureeArchivage, categoriesDestinataires,
                raisonTransfertVersCatDestinataire, Boolean.parseBoolean(transfertHorsUE), paysDestinataire, commentaires);
    }

    /**
     * Construit la définition correspondant à une cellule du fichier importé.
     * Retourne {@code null} si la cellule est vide, afin de laisser la
     * référence nulle en base plutôt que de créer une définition sans valeur.
     */
    private static DefinitionDTO definition(String type, String valeur) {
        if (StringUtils.isBlank(valeur)) {
            return null;
        }
        return new DefinitionDTO(null, type, valeur);
    }

    /**
     * Construit la durée correspondant à une cellule du fichier importé.
     * Retourne {@code null} si la cellule est vide, afin de laisser la
     * référence nulle en base plutôt que de créer une durée sans valeur.
     */
    private static DureeDTO duree(boolean estArchivage, String valeur) {
        if (StringUtils.isBlank(valeur)) {
            return null;
        }
        return new DureeDTO(null, estArchivage, valeur);
    }

    /**
     * Construit le responsable de traitement correspondant à une cellule du
     * fichier importé. Les informations complémentaires ne figurent pas dans le
     * fichier : elles restent nulles jusqu'à leur saisie via l'API.
     */
    private static ResponsableTraitementDTO responsable(String valeur) {
        if (StringUtils.isBlank(valeur)) {
            return null;
        }
        return new ResponsableTraitementDTO(null, valeur, null);
    }
}
