package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.EtablissementDTO;
import com.minds.rgpd.business.dtos.ProfilDTO;
import com.minds.rgpd.business.dtos.UtilisateurDTO;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ControllersIT extends AbstractITSpring {

    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    private static <T> List<T> stringToList(String contentAsString, Class<T> classe) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<JsonElement> content = JsonParser.parseString(contentAsString).getAsJsonArray().asList();
        return content.stream().map(jsonElement -> {
            try {
                return objectMapper.readValue(jsonElement.toString(), classe);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    /**
     * Meme lecture que {@link #stringToList}, pour un endpoint pagine : les
     * elements sont sous la propriete {@code content} de la page.
     */
    private static <T> List<T> stringToPageContent(String contentAsString, Class<T> classe) {
        return stringToList(
                JsonParser.parseString(contentAsString).getAsJsonObject().getAsJsonArray("content").toString(),
                classe);
    }

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getClients() throws Exception {
        // GIVEN

        // WHEN
        String result = mockMvc.perform(get("/clients")).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(result).isNotNull().isNotBlank();
        List<ClientDTO> clientsList = stringToList(result, ClientDTO.class);
        assertThat(clientsList).isNotNull().isNotEmpty().hasSize(3);
    }

    @Test
    void getProfils() throws Exception {
        // GIVEN

        // WHEN
        String result = mockMvc.perform(get("/profils")).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(result).isNotNull().isNotBlank();
        List<ProfilDTO> profilList = stringToList(result, ProfilDTO.class);
        assertThat(profilList).isNotNull().isNotEmpty().hasSize(3);
    }

    @Test
    void getUtilisateurs() throws Exception {
        // GIVEN

        // WHEN
        String result = mockMvc.perform(get("/utilisateurs")).andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(result).isNotNull().isNotBlank();
        List<UtilisateurDTO> utilisateursList = stringToList(result, UtilisateurDTO.class);
        assertThat(utilisateursList).isNotNull().isNotEmpty().hasSize(3);
    }

    @Test
    void getEtablissements() throws Exception {
        // GIVEN un utilisateur rattaché à un seul client
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .claim("preferred_username", "alice")
                .claim("client_groups", List.of("La breteche"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        try {
            // WHEN
            String result = mockMvc.perform(get("/etablissements")).andReturn().getResponse().getContentAsString();

            // THEN : seuls les établissements du client porté par le jeton sont listés
            assertThat(result).isNotNull().isNotBlank();
            List<EtablissementDTO> etablissementsList = stringToPageContent(result, EtablissementDTO.class);
            assertThat(etablissementsList).extracting(EtablissementDTO::nom)
                    .containsExactly("Agence Lyon", "Siège Paris");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
