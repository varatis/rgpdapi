package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.persistence.entities.Preconisation;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@Sql(scripts = "classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class ImportPreconisationIT extends AbstractITSpring {

    ObjectMapper mapper = new ObjectMapper();

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
    void importFichierAvecOngletPreconisations() throws Exception {
        String filename = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";
        byte[] fileContent;
        try (InputStream input = new ClassPathResource("rgpdFile/" + filename).getInputStream();
             Workbook workbook = new XSSFWorkbook(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Préconisations");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Préconisation");
            header.createCell(1).setCellValue("Explication");
            header.createCell(2).setCellValue("Risque encouru");
            header.createCell(3).setCellValue("Contraintes");
            header.createCell(4).setCellValue("Cout");
            header.createCell(5).setCellValue("Priorité");
            header.createCell(6).setCellValue("Complexité");
            header.createCell(7).setCellValue("Commentaire");
            header.createCell(8).setCellValue("État d'avancement");
            header.createCell(9).setCellValue("ID");

            Row liee = sheet.createRow(1);
            liee.createCell(0).setCellValue("Créer une adresse mail DPO importée");
            liee.createCell(1).setCellValue("Adresse générique DPO");
            liee.createCell(2).setCellValue("Droits IL");
            liee.createCell(3).setCellValue("Créer une BAL");
            liee.createCell(4).setCellValue("Aucun");
            liee.createCell(5).setCellValue("1 : Très urgent");
            liee.createCell(6).setCellValue("1 : Très simple");
            liee.createCell(8).setCellValue("En cours");
            liee.createCell(9).setCellValue(1);

            Row catalogue = sheet.createRow(2);
            catalogue.createCell(0).setCellValue("Fermer les bureaux importé");
            catalogue.createCell(1).setCellValue("Fermer dès absence");
            catalogue.createCell(5).setCellValue("1 : Très urgent");
            catalogue.createCell(6).setCellValue("1 : Très simple");

            workbook.write(output);
            fileContent = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileContent
        );

        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd").file(file))
                .andReturn().getResponse().getContentAsString();

        InfoFichierDTO infoFichierDTO = mapper.readValue(infoFichierDTOString, InfoFichierDTO.class);
        assertThat(infoFichierDTO.statusFichier()).contains("OK");

        List<Preconisation> importees = preconisationRepository.findAll().stream()
                .filter(p -> p.getLibelle() != null && (p.getLibelle().endsWith("importée") || p.getLibelle().endsWith("importé")))
                .toList();

        assertThat(importees).hasSize(2);

        Preconisation avecAvancement = importees.stream()
                .filter(p -> "Créer une adresse mail DPO importée".equals(p.getLibelle()))
                .findFirst()
                .orElseThrow();
        assertThat(avecAvancement.getEtatAvancement()).isEqualTo("En cours");
        assertThat(avecAvancement.getPriorite()).isEqualTo("1 : Très urgent");
        assertThat(avecAvancement.getTraitement()).isNotNull();
        assertThat(avecAvancement.getTraitement().getIdFonctionnel()).isEqualTo(1);
        assertThat(avecAvancement.getClient().getNom()).isEqualTo("La breteche");

        Preconisation sansTraitement = importees.stream()
                .filter(p -> "Fermer les bureaux importé".equals(p.getLibelle()))
                .findFirst()
                .orElseThrow();
        assertThat(sansTraitement.getEtatAvancement()).isNull();
        assertThat(sansTraitement.getTraitement()).isNull();
    }
}
