package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.dtos.TraitementPartielDTO;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TraitementControllerIT extends AbstractITSpring {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    private static List<TraitementPartielDTO> stringToTraitementPartielList(String contentAsString) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<JsonElement> content = JsonParser.parseString(contentAsString).getAsJsonObject().getAsJsonArray("content").asList();
        return content.stream().map(jsonElement -> {
            try {
                return objectMapper.readValue(jsonElement.toString(), TraitementPartielDTO.class);
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
    void testRecupOneTraitement() throws Exception {
        // GIVEN

        // WHEN
        String traitementDTOString = mockMvc.perform(get("/traitements/1")).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(traitementDTOString).isNotNull().isNotEmpty();
        TraitementDTO traitementDTO = mapper.readValue(traitementDTOString, TraitementDTO.class);
        assertThat(traitementDTO).isNotNull();
        assertThat(traitementDTO.idFonctionnel()).isNotNull().isEqualTo(1);
    }

    @Test
    void testRecupPartialTraitements() throws Exception {
        // GIVEN

        // WHEN
        String traitementsPartielDTOPage = mockMvc.perform(get("/traitements").param("page", "0").param("size", "10")).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(traitementsPartielDTOPage).isNotNull();
        List<TraitementPartielDTO> partielDTOS = stringToTraitementPartielList(traitementsPartielDTOPage);
        partielDTOS.forEach(traitementPartielDTO -> {
            assertThat(traitementPartielDTO.idFonctionnel()).isNotNull();
            assertThat(traitementPartielDTO.nom()).isNotNull().isNotBlank();
            assertThat(traitementPartielDTO.gestionnaire()).isNotNull().isNotBlank();
            assertThat(traitementPartielDTO.finalitePrincipale()).isNotNull().isNotBlank();
        });
    }

    @Test
    void createTraitement() throws Exception {
        // GIVEN
        TraitementDTO traitementDTO = TraitementDTO.builder()
                .idFonctionnel(1)
                .nom("Traitement Test")
                .dateIdentification(LocalDate.now())
                .client(ClientDTO.builder().id(UUID.fromString("0e4bf889-fea0-46ac-894d-ca39cbf00359")).build())
                .build();
        ObjectMapper objetMapper = new ObjectMapper();
        objetMapper.registerModule(new JavaTimeModule());
        String traitementDtoString = objetMapper.writeValueAsString(traitementDTO);

        // WHEN
        MvcResult mvcResult = mockMvc.perform(post("/traitements").content(traitementDtoString).contentType(MediaType.APPLICATION_JSON)).andReturn();

        // THEN
        assertThat(mvcResult).isNotNull();
        assertThat(mvcResult.getResponse()).isNotNull();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void createTraitementNoClient() throws Exception {
        // GIVEN
        TraitementDTO traitementDTO = TraitementDTO.builder()
                .idFonctionnel(1)
                .nom("Traitement Test")
                .dateIdentification(LocalDate.now())
                .client(ClientDTO.builder().id(UUID.fromString("aaaaaaaa-fea0-46ac-894d-ca39cbf00359")).build())
                .build();
        ObjectMapper objetMapper = new ObjectMapper();
        objetMapper.registerModule(new JavaTimeModule());
        String traitementDtoString = objetMapper.writeValueAsString(traitementDTO);

        // WHEN
        MvcResult mvcResult = mockMvc.perform(post("/traitements").content(traitementDtoString).contentType(MediaType.APPLICATION_JSON)).andReturn();

        // THEN
        assertThat(mvcResult).isNotNull();
        assertThat(mvcResult.getResponse()).isNotNull();
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void getNextIdTraitement() throws Exception {
        // GIVEN

        // WHEN
        MvcResult mvcResult = mockMvc.perform(get("/traitements/nextId")).andReturn();

        // THEN
        assertThat(Integer.parseInt(mvcResult.getResponse().getContentAsString())).isEqualTo(4);
    }
}
