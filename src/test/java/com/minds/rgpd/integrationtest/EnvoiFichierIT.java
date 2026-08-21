package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.persistence.entities.DefinitionChamp;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.DefinitionChampRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
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

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@Sql(scripts = "classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class EnvoiFichierIT extends AbstractITSpring {

    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    TraitementRepository traitementRepository;
    @Autowired
    DefinitionChampRepository definitionChampRepository;
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
        String filename = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";
        File realFile = new ClassPathResource("rgpdFile/" + filename).getFile();
        byte[] fileContent = Files.readAllBytes(realFile.toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                realFile.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                fileContent
        );


        // WHEN
        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd").file(file)).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(infoFichierDTOString).isNotNull();
        InfoFichierDTO infoFichierDTO = mapper.readValue(infoFichierDTOString, InfoFichierDTO.class);
        assertThat(infoFichierDTO.nomFichier()).isEqualTo(filename);
        assertThat(infoFichierDTO.statusFichier()).isEqualTo("OK");
        List<Traitement> traitementList = traitementRepository.findAll();
        assertThat(traitementList).isNotNull().isNotEmpty().hasSize(83);

        // L'onglet FR_Définitions est importé en base avec le registre
        // (31 champs définis ; « Dispositions existantes… » cible 2 colonnes)
        List<DefinitionChamp> definitions =
                definitionChampRepository.findByClientNomOrderByOrdreAsc("La breteche");
        assertThat(definitions).hasSize(32);
        assertThat(definitions).allSatisfy(d -> {
            assertThat(d.getEdition()).isEqualTo("3");
            assertThat(d.getDefinition()).isNotBlank();
        });
        assertThat(definitions)
                .filteredOn(d -> d.getLibelle().equals("Nom du traitement"))
                .singleElement()
                .satisfies(d -> {
                    assertThat(d.getSection()).isEqualTo("Identification du traitement");
                    assertThat(d.getTableCible()).isEqualTo("traitement");
                    assertThat(d.getColonneCible()).isEqualTo("nom");
                });
        // Définitions sans correspondance BDD : importées quand même, cibles nulles
        assertThat(definitions)
                .filteredOn(d -> d.getColonneCible() == null && d.getTableCible() == null)
                .extracting(DefinitionChamp::getLibelle)
                .containsExactlyInAnyOrder(
                        "Responsable(s) conjoint(s) du traitement",
                        "Applications support du traitement");
    }
}
