package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@Sql(scripts = "classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class EnvoiFichierIT extends AbstractITSpring {

    private static final String FICHIER_REGISTRE = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";
    private static final String CLIENT_BRETECHE = "La breteche";

    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    TraitementRepository traitementRepository;
    @Autowired
    PreconisationRepository preconisationRepository;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void importFichier() throws Exception {
        // GIVEN
        // Le jeu de données contient 2 traitements et 1 préconisation du client
        // "La breteche", qui doivent disparaître au profit du contenu du fichier (RG2).
        MockMultipartFile file = fichierRegistre(FICHIER_REGISTRE);

        // WHEN
        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd").file(file)).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(infoFichierDTOString).isNotNull();
        InfoFichierDTO infoFichierDTO = mapper.readValue(infoFichierDTOString, InfoFichierDTO.class);
        assertThat(infoFichierDTO.nomFichier()).isEqualTo(FICHIER_REGISTRE);
        assertThat(infoFichierDTO.statusFichier()).isEqualTo("OK");

        // RG2 : le registre du client est remplacé par le contenu du fichier.
        // Le fichier porte 80 traitements ; le traitement "Conformité RGPD" d'un
        // autre client (Entreprise Alpha) doit être conservé.
        List<Traitement> traitementList = traitementRepository.findAll();
        assertThat(traitementList).isNotNull().isNotEmpty().hasSize(81);

        // RG2 : les traitements antérieurs du client importé n'existent plus...
        assertThat(traitementList)
                .noneMatch(traitement -> "Gestion des salariés".equals(traitement.getNom()))
                .noneMatch(traitement -> "Suivi des ventes".equals(traitement.getNom()));
        // ...mais les données des autres clients restent en place (CA5).
        assertThat(traitementList)
                .anyMatch(traitement -> "Conformité RGPD".equals(traitement.getNom()));

        // RG2 : les préconisations du client sont remplacées elles aussi. Le
        // fichier n'embarque pas de feuille de préconisations : il n'en reste aucune.
        assertThat(preconisationRepository.findAll()).isEmpty();

        // La version issue du nom du fichier (ed3) est portée par les traitements importés.
        assertThat(traitementList)
                .filteredOn(traitement -> CLIENT_BRETECHE.equals(traitement.getClient().getNom()))
                .allMatch(traitement -> Integer.valueOf(3).equals(traitement.getVersion()));
    }

    @Test
    void importFichierAvecSuiviDesPreconisations() throws Exception {
        // GIVEN : le fichier du registre est enrichi d'une feuille "Suivi des
        // préconisations" (RG5 : les colonnes complémentaires sont importées).
        byte[] contenu = enrichirAvecSuiviDesPreconisations(FICHIER_REGISTRE);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                FICHIER_REGISTRE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                contenu
        );

        // WHEN
        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd").file(file)).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(mapper.readValue(infoFichierDTOString, InfoFichierDTO.class).statusFichier()).isEqualTo("OK");

        List<Preconisation> preconisations = preconisationRepository.findAll();
        // RG2 : l'ancienne préconisation du client est remplacée par celles du fichier.
        assertThat(preconisations).hasSize(2);
        assertThat(preconisations)
                .noneMatch(preconisation -> "Préconisation à remplacer".equals(preconisation.getLibelle()));

        // CA5 : le lien vers le traitement se résout sur les traitements
        // nouvellement importés (recherche par ID du fichier).
        Preconisation chiffrement = preconisations.stream()
                .filter(preconisation -> "Chiffrer les sauvegardes".equals(preconisation.getLibelle()))
                .findFirst()
                .orElseThrow();
        assertThat(chiffrement.getTraitement()).isNotNull();
        assertThat(chiffrement.getTraitement().getIdFonctionnel()).isEqualTo(1);
        assertThat(chiffrement.getTraitement().getNom()).isEqualTo("Pré inscription");

        // Une préconisation sans traitement identifié reste globale au client.
        Preconisation sensibilisation = preconisations.stream()
                .filter(preconisation -> "Sensibiliser les équipes".equals(preconisation.getLibelle()))
                .findFirst()
                .orElseThrow();
        assertThat(sensibilisation.getTraitement()).isNull();
    }

    @Test
    void importFichierInvalide_neRemplacePasLeRegistre() throws Exception {
        // GIVEN : le fichier ne respecte pas le format attendu (colonne
        // "Nom du traitement" absente de l'en-tête).
        byte[] contenu = supprimerColonneObligatoire(FICHIER_REGISTRE, "Nom du traitement");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                FICHIER_REGISTRE,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                contenu
        );

        // WHEN
        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd").file(file)).andReturn().getResponse().getContentAsString();

        // THEN
        // RG3 : l'utilisateur est informé de l'échec...
        InfoFichierDTO infoFichierDTO = mapper.readValue(infoFichierDTOString, InfoFichierDTO.class);
        assertThat(infoFichierDTO.statusFichier()).isNotEqualTo("OK");

        // ...et le registre du client reste inchangé (CA5) : les 3 traitements et
        // la préconisation du jeu de données initial sont toujours là.
        assertThat(traitementRepository.findAll()).hasSize(3);
        assertThat(preconisationRepository.findAll())
                .anyMatch(preconisation -> "Préconisation à remplacer".equals(preconisation.getLibelle()));
    }

    private MockMultipartFile fichierRegistre(String filename) throws Exception {
        File realFile = new ClassPathResource("rgpdFile/" + filename).getFile();
        byte[] fileContent = Files.readAllBytes(realFile.toPath());
        return new MockMultipartFile(
                "file",
                realFile.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileContent
        );
    }

    /** Ajoute une feuille "Suivi des préconisations" au registre importé. */
    private byte[] enrichirAvecSuiviDesPreconisations(String filename) throws Exception {
        try (InputStream inputStream = new ClassPathResource("rgpdFile/" + filename).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            Row entete = workbook.createSheet("Suivi des préconisations").createRow(0);
            entete.createCell(0).setCellValue("ID");
            entete.createCell(1).setCellValue("Préconisation");
            entete.createCell(2).setCellValue("Explication");
            entete.createCell(3).setCellValue("État d'avancement");

            Row ligne1 = workbook.getSheet("Suivi des préconisations").createRow(1);
            ligne1.createCell(0).setCellValue(1);
            ligne1.createCell(1).setCellValue("Chiffrer les sauvegardes");
            ligne1.createCell(2).setCellValue("Les sauvegardes sont en clair");
            ligne1.createCell(3).setCellValue("À faire");

            Row ligne2 = workbook.getSheet("Suivi des préconisations").createRow(2);
            ligne2.createCell(1).setCellValue("Sensibiliser les équipes");
            ligne2.createCell(3).setCellValue("En cours");

            workbook.write(sortie);
            return sortie.toByteArray();
        }
    }

    /** Supprime une colonne obligatoire de l'en-tête du registre pour rendre le fichier invalide. */
    private byte[] supprimerColonneObligatoire(String filename, String libelleColonne) throws Exception {
        try (InputStream inputStream = new ClassPathResource("rgpdFile/" + filename).getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {

            // L'en-tête du registre se situe sur la 6e ligne (index 5).
            Row entete = workbook.getSheet("Registre de traitement").getRow(5);
            for (int i = 0; i < entete.getLastCellNum(); i++) {
                if (entete.getCell(i) != null && libelleColonne.equals(entete.getCell(i).getStringCellValue().trim())) {
                    entete.removeCell(entete.getCell(i));
                    break;
                }
            }

            workbook.write(sortie);
            return sortie.toByteArray();
        }
    }
}
