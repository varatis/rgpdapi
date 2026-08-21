package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.PreconisationDTO;
import com.minds.rgpd.business.dtos.PreconisationPartielDTO;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PreconisationControllerIT extends AbstractITSpring {

    private static final UUID PRECONISATION_EN_COURS = UUID.fromString("aaaaaaaa-1111-4111-8111-111111111111");
    private static final UUID TRAITEMENT_SALARIES = UUID.fromString("31b8d234-2346-4761-89fb-92d24f49bb96");
    private static final UUID INCONNUE = UUID.fromString("bbbbbbbb-1111-4111-8111-111111111111");

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    private static List<PreconisationPartielDTO> stringToPartielList(String contentAsString) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<JsonElement> content = JsonParser.parseString(contentAsString).getAsJsonObject().getAsJsonArray("content").asList();
        return content.stream().map(jsonElement -> {
            try {
                return objectMapper.readValue(jsonElement.toString(), PreconisationPartielDTO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void listeSansClient_retournePageVide() throws Exception {
        String body = mockMvc.perform(get("/preconisations").param("page", "0").param("size", "10"))
                .andReturn().getResponse().getContentAsString();

        assertThat(stringToPartielList(body)).isEmpty();
    }

    @Test
    void listeParClient_afficheLesPreconisationsEtLeurAvancement() throws Exception {
        String body = mockMvc.perform(get("/preconisations")
                        .param("clientNom", "La breteche")
                        .param("page", "0")
                        .param("size", "10"))
                .andReturn().getResponse().getContentAsString();

        List<PreconisationPartielDTO> preconisations = stringToPartielList(body);
        assertThat(preconisations).hasSize(2);
        assertThat(preconisations)
                .extracting(PreconisationPartielDTO::etatAvancement)
                .containsExactlyInAnyOrder("En cours", "À faire");
        assertThat(preconisations)
                .extracting(PreconisationPartielDTO::libelle)
                .contains("Créer une adresse mail DPO", "Fermer les bureaux");
    }

    @Test
    void listeFiltreParEtatAvancement() throws Exception {
        String body = mockMvc.perform(get("/preconisations")
                        .param("clientNom", "La breteche")
                        .param("etatAvancement", "En cours"))
                .andReturn().getResponse().getContentAsString();

        List<PreconisationPartielDTO> preconisations = stringToPartielList(body);
        assertThat(preconisations).hasSize(1);
        assertThat(preconisations.getFirst().etatAvancement()).isEqualTo("En cours");
    }

    @Test
    void listeFiltreParTraitement() throws Exception {
        String body = mockMvc.perform(get("/preconisations")
                        .param("clientNom", "La breteche")
                        .param("idTraitement", TRAITEMENT_SALARIES.toString()))
                .andReturn().getResponse().getContentAsString();

        List<PreconisationPartielDTO> preconisations = stringToPartielList(body);
        assertThat(preconisations).hasSize(2);
        assertThat(preconisations)
                .extracting(PreconisationPartielDTO::traitementIdentifiant)
                .containsOnly(TRAITEMENT_SALARIES);
    }

    @Test
    void detail_afficheEtatAvancement() throws Exception {
        String body = mockMvc.perform(get("/preconisations/" + PRECONISATION_EN_COURS))
                .andReturn().getResponse().getContentAsString();

        PreconisationDTO dto = mapper.readValue(body, PreconisationDTO.class);
        assertThat(dto.identifiant()).isEqualTo(PRECONISATION_EN_COURS);
        assertThat(dto.libelle()).isEqualTo("Créer une adresse mail DPO");
        assertThat(dto.etatAvancement()).isEqualTo("En cours");
        assertThat(dto.priorite()).isEqualTo("1 : Très urgent");
        assertThat(dto.traitementIdentifiant()).isEqualTo(TRAITEMENT_SALARIES);
        assertThat(dto.traitementNom()).isEqualTo("Gestion des salariés");
    }

    @Test
    void detailInconnu_retourne404() throws Exception {
        int status = mockMvc.perform(get("/preconisations/" + INCONNUE))
                .andReturn().getResponse().getStatus();

        assertThat(status).isEqualTo(404);
    }
}
