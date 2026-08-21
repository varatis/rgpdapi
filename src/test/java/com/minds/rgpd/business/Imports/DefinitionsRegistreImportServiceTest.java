package com.minds.rgpd.business.Imports;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.DefinitionChamp;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du parseur de l'onglet « FR_Définitions » sur le fichier registre réel
 * (édition 3.25) présent dans les ressources de test.
 * <p>
 * Test sans contexte Spring ni base : le parseur est un composant sans dépendance.
 */
class DefinitionsRegistreImportServiceTest {

    private static final String FICHIER_REGISTRE =
            "rgpdFile/La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";

    private final DefinitionsRegistreImportService service = new DefinitionsRegistreImportService();

    private final Client client = Client.builder()
            .id(UUID.randomUUID())
            .nom("La breteche")
            .build();

    private DefinitionsRegistreImportService.ResultatExtraction extraireFichierReel() throws IOException {
        try (XSSFWorkbook workbook =
                     new XSSFWorkbook(new ClassPathResource(FICHIER_REGISTRE).getInputStream())) {
            return service.extraire(workbook, client, "3");
        }
    }

    @Test
    void extrait_toutes_les_definitions_de_l_onglet() throws IOException {
        DefinitionsRegistreImportService.ResultatExtraction resultat = extraireFichierReel();

        assertThat(resultat.ongletPresent()).isTrue();

        // 31 champs définis dans l'onglet ; « Dispositions existantes… » cible 2 colonnes
        // (physique + numérique) et produit donc 2 lignes.
        assertThat(resultat.definitions()).hasSize(32);
    }

    @Test
    void propage_client_edition_section_et_ordre() throws IOException {
        List<DefinitionChamp> definitions = extraireFichierReel().definitions();

        assertThat(definitions).allSatisfy(d -> {
            assertThat(d.getClient()).isEqualTo(client);
            assertThat(d.getEdition()).isEqualTo("3");
            assertThat(d.getOrdre()).isNotNull().isPositive();
            assertThat(d.getLibelle()).isNotBlank();
            assertThat(d.getDefinition()).isNotBlank();
        });

        DefinitionChamp id = definitions.get(0);
        assertThat(id.getLibelle()).isEqualTo("ID");
        assertThat(id.getSection()).isEqualTo("Identification du traitement");
        assertThat(id.getOrdre()).isEqualTo(5); // ligne 5 de l'onglet
        assertThat(id.getDefinition()).contains("Numéro unique d'identification du traitement");
    }

    @Test
    void resout_la_correspondance_avec_les_colonnes_bdd() throws IOException {
        List<DefinitionChamp> definitions = extraireFichierReel().definitions();

        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Nom du traitement"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.getTableCible()).isEqualTo("traitement");
                    assertThat(d.getColonneCible()).isEqualTo("nom");
                });

        // Champ portant sur la relation N-N : table ciblée, colonne nulle
        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Etablissement(s)"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.getTableCible()).isEqualTo("traitement_etablissement");
                    assertThat(d.getColonneCible()).isNull();
                });

        // Libellé dupliqué dans le registre : une ligne par colonne cible
        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Dispositions existantes pour assurer la sécurité des données"))
                .extracting(DefinitionChamp::getColonneCible)
                .containsExactlyInAnyOrder(
                        "dispositions_securite_donnees_physique",
                        "dispositions_securite_donnees_numerique");

        // Correspondance assumée malgré l'écart de libellé (cf. docs/mapping-bdd-registre.md, I6)
        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Durée d'archivage courant"))
                .singleElement()
                .satisfies(d -> assertThat(d.getColonneCible()).isEqualTo("duree_conservation"));
    }

    @Test
    void conserve_les_definitions_sans_correspondance_bdd() throws IOException {
        List<DefinitionChamp> definitions = extraireFichierReel().definitions();

        // Définitions orphelines (cf. docs/mapping-bdd-registre.md, I1/I2) :
        // importées avec des cibles nulles pour rester visibles en base.
        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Responsable(s) conjoint(s) du traitement"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.getTableCible()).isNull();
                    assertThat(d.getColonneCible()).isNull();
                });

        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Applications support du traitement"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.getTableCible()).isNull();
                    assertThat(d.getColonneCible()).isNull();
                });
    }

    @Test
    void ignore_les_listes_de_reference_en_colonne_c_seule() throws IOException {
        List<DefinitionChamp> definitions = extraireFichierReel().definitions();

        // Les lignes 43 à 76 de l'onglet (listes « données sensibles », « licéité article 6 »,
        // « PIA obligatoire ») n'ont pas de libellé en colonne B et ne doivent pas apparaître.
        assertThat(definitions)
                .extracting(DefinitionChamp::getLibelle)
                .noneMatch(libelle -> libelle.contains("NIR (N° SS)"));
    }

    @Test
    void onglet_absent_sans_exception() {
        try (XSSFWorkbook workbookVide = new XSSFWorkbook()) {
            DefinitionsRegistreImportService.ResultatExtraction resultat =
                    service.extraire(workbookVide, client, "1");

            assertThat(resultat.ongletPresent()).isFalse();
            assertThat(resultat.definitions()).isEmpty();
        } catch (IOException e) {
            throw new AssertionError("Fermeture du workbook en échec", e);
        }
    }
}
