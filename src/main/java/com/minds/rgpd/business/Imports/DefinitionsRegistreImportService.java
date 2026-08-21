package com.minds.rgpd.business.Imports;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.DefinitionChamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Extrait les définitions métier de l'onglet « FR_Définitions » d'un fichier registre.
 * <p>
 * Contrairement à {@link ExcelImportService}, cet onglet n'a pas de ligne d'en-tête :
 * son gabarit est fixe (libellé en colonne B, définition en colonne C). Une ligne avec
 * un libellé mais sans définition marque le début d'une section (ex. : « Identification
 * du traitement ») ; les lignes sans libellé (listes de référence en colonne C seule)
 * sont ignorées.
 * <p>
 * Les valeurs extraites sont persistées dans la table {@code definition_champ}
 * (remplacement complet par client et édition à chaque import) — aucune définition
 * métier n'est codée en dur dans l'application.
 */
@Slf4j
@Component
public class DefinitionsRegistreImportService {

    public static final String SHEET_NAME = "FR_Définitions";

    private static final int COLONNE_LIBELLE = 1;    // colonne B
    private static final int COLONNE_DEFINITION = 2; // colonne C

    /**
     * Extrait les définitions de l'onglet FR_Définitions du classeur.
     *
     * @param workbook classeur du fichier registre importé
     * @param client   client propriétaire du fichier (déduit du nom de fichier)
     * @param edition  édition du fichier, extraite de son nom
     * @return le résultat de l'extraction (onglet absent → {@code ongletPresent() == false})
     */
    public ResultatExtraction extraire(Workbook workbook, Client client, String edition) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            log.warn("Onglet '{}' absent du fichier : définitions métier non importées", SHEET_NAME);
            return ResultatExtraction.ongletAbsent();
        }

        List<DefinitionChamp> definitions = new ArrayList<>();
        String sectionCourante = null;

        for (Row row : sheet) {
            String libelle = lireCellule(row, COLONNE_LIBELLE);
            String definition = lireCellule(row, COLONNE_DEFINITION);

            if (libelle.isBlank()) {
                continue;
            }
            if (definition.isBlank()) {
                // Libellé sans définition = titre de l'onglet ou en-tête de section
                sectionCourante = libelle;
                continue;
            }

            List<CorrespondanceChampRegistre.Cible> cibles =
                    CorrespondanceChampRegistre.resoudre(libelle)
                            // Libellé inconnu de la correspondance : on importe quand même
                            // la définition, sans cible — l'écart reste visible en base.
                            .orElseGet(() -> List.of(new CorrespondanceChampRegistre.Cible(null, null)));

            for (CorrespondanceChampRegistre.Cible cible : cibles) {
                definitions.add(DefinitionChamp.builder()
                        .client(client)
                        .edition(edition)
                        .section(sectionCourante)
                        .libelle(libelle)
                        .definition(definition)
                        .tableCible(cible.tableCible())
                        .colonneCible(cible.colonneCible())
                        .ordre(row.getRowNum() + 1) // +1 : numéro de ligne Excel (1-based)
                        .build());
            }
        }

        log.info("Onglet '{}' : {} définition(s) extraite(s) (dont {} sans correspondance BDD)",
                SHEET_NAME,
                definitions.size(),
                definitions.stream().filter(d -> d.getTableCible() == null).count());

        return new ResultatExtraction(true, definitions);
    }

    private String lireCellule(Row row, int indexColonne) {
        Cell cell = row.getCell(indexColonne);
        if (cell == null) {
            return "";
        }
        // Normalisation : espaces insécables (\u00A0) saisis dans Excel + marges parasites.
        return cell.toString().replace('\u00A0', ' ').trim();
    }

    /**
     * Résultat de l'extraction des définitions.
     *
     * @param ongletPresent faux si l'onglet FR_Définitions est absent du fichier
     * @param definitions   définitions extraites (prêtes à persister)
     */
    public record ResultatExtraction(boolean ongletPresent, List<DefinitionChamp> definitions) {

        static ResultatExtraction ongletAbsent() {
            return new ResultatExtraction(false, List.of());
        }
    }
}
