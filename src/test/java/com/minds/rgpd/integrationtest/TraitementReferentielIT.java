package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.RgpdApplication;
import com.minds.rgpd.testcontainers.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Verifie de bout en bout, contre une vraie base, la creation et la modification
 * d'un traitement portant des valeurs de referentiel (definition, duree,
 * responsable de traitement).
 * <p>
 * Le referentiel etant partage entre les traitements d'un meme client, le point
 * sensible est la modification : elle doit reaffecter la reference du traitement
 * sans jamais reecrire la ligne pointee.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = RgpdApplication.class)
@Import(TestContainersConfiguration.class)
@ActiveProfiles("test")
@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
class TraitementReferentielIT {

    private static final String CLIENT_BRETECHE = "0e4bf889-fea0-46ac-894d-ca39cbf00359";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String corpsTraitement(int idFonctionnel, String nom, String finalite,
                                   String conservation, String responsable) {
        return """
                {
                  "idFonctionnel": %d,
                  "nom": "%s",
                  "dateIdentification": "2026-02-01",
                  "client": { "id": "%s" },
                  "finalitePrincipale": { "type": "Finalite Principale", "valeur": "%s" },
                  "dureeConservation": { "estArchivage": false, "valeur": "%s" },
                  "responsableTraitement": { "valeur": "%s" }
                }
                """.formatted(idFonctionnel, nom, CLIENT_BRETECHE, finalite, conservation, responsable);
    }

    private JsonNode envoyer(MockHttpServletRequestBuilder requete, String corps, int statutAttendu) throws Exception {
        var reponse = mockMvc.perform(requete.content(corps).contentType(MediaType.APPLICATION_JSON))
                .andReturn().getResponse();
        assertThat(reponse.getStatus())
                .as("corps de reponse : %s", reponse.getContentAsString())
                .isEqualTo(statutAttendu);
        return mapper.readTree(reponse.getContentAsString());
    }

    private JsonNode lire(int idFonctionnel) throws Exception {
        var reponse = mockMvc.perform(get("/traitements/" + idFonctionnel)).andReturn().getResponse();
        assertThat(reponse.getStatus()).isEqualTo(200);
        return mapper.readTree(reponse.getContentAsString());
    }

    private Integer compterDefinitions(String valeur) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM definition WHERE valeur = ? AND client_id = ?::uuid",
                Integer.class, valeur, CLIENT_BRETECHE);
    }

    @Test
    void creeUnTraitementAvecSesValeursDeReferentiel() throws Exception {
        JsonNode cree = envoyer(post("/traitements"),
                corpsTraitement(10, "Gestion des acces", "Securite du SI", "3 ans", "RSSI"), 201);

        assertThat(cree.path("finalitePrincipale").path("valeur").asText()).isEqualTo("Securite du SI");
        assertThat(cree.path("finalitePrincipale").path("id").isNull()).isFalse();
        assertThat(cree.path("dureeConservation").path("valeur").asText()).isEqualTo("3 ans");
        assertThat(cree.path("responsableTraitement").path("valeur").asText()).isEqualTo("RSSI");

        // La valeur est bien relue depuis la base par le detail
        JsonNode relu = lire(10);
        assertThat(relu.path("finalitePrincipale").path("valeur").asText()).isEqualTo("Securite du SI");
        assertThat(relu.path("dureeConservation").path("valeur").asText()).isEqualTo("3 ans");
        assertThat(relu.path("responsableTraitement").path("valeur").asText()).isEqualTo("RSSI");
    }

    @Test
    void reutiliseLaValeurDeReferentielDejaEnregistree() throws Exception {
        envoyer(post("/traitements"),
                corpsTraitement(11, "Premier", "Valeur partagee", "3 ans", "RSSI"), 201);
        envoyer(post("/traitements"),
                corpsTraitement(12, "Second", "Valeur partagee", "3 ans", "RSSI"), 201);

        assertThat(compterDefinitions("Valeur partagee")).isEqualTo(1);
        assertThat(lire(11).path("finalitePrincipale").path("id").asInt())
                .isEqualTo(lire(12).path("finalitePrincipale").path("id").asInt());
    }

    @Test
    void modifieUnTraitementSansAltererLesAutres() throws Exception {
        envoyer(post("/traitements"),
                corpsTraitement(13, "Premier", "Valeur partagee", "3 ans", "RSSI"), 201);
        envoyer(post("/traitements"),
                corpsTraitement(14, "Second", "Valeur partagee", "3 ans", "RSSI"), 201);

        envoyer(put("/traitements/13"),
                corpsTraitement(13, "Premier", "Valeur renommee", "7 ans", "DPO"), 200);

        // Le traitement modifie pointe la nouvelle valeur...
        JsonNode modifie = lire(13);
        assertThat(modifie.path("finalitePrincipale").path("valeur").asText()).isEqualTo("Valeur renommee");
        assertThat(modifie.path("dureeConservation").path("valeur").asText()).isEqualTo("7 ans");
        assertThat(modifie.path("responsableTraitement").path("valeur").asText()).isEqualTo("DPO");

        // ...et celui qui partageait l'ancienne n'a pas bouge
        JsonNode intact = lire(14);
        assertThat(intact.path("finalitePrincipale").path("valeur").asText()).isEqualTo("Valeur partagee");
        assertThat(intact.path("dureeConservation").path("valeur").asText()).isEqualTo("3 ans");
        assertThat(intact.path("responsableTraitement").path("valeur").asText()).isEqualTo("RSSI");

        // La ligne de referentiel d'origine existe toujours, inchangee
        assertThat(compterDefinitions("Valeur partagee")).isEqualTo(1);
        assertThat(compterDefinitions("Valeur renommee")).isEqualTo(1);
    }

    @Test
    void modifieUnTraitementPreexistant() throws Exception {
        // Traitement issu du jeu de donnees, donc anterieur a la requete : sa
        // finalite est deja une ligne de referentiel, ses durees sont nulles.
        envoyer(put("/traitements/1"),
                corpsTraitement(1, "Gestion des salaries", "Administration RH", "20 ans", "RSSI"), 200);

        JsonNode modifie = lire(1);
        // La finalite est inchangee : elle doit etre reutilisee, pas dupliquee
        assertThat(modifie.path("finalitePrincipale").path("valeur").asText()).isEqualTo("Administration RH");
        assertThat(compterDefinitions("Administration RH")).isEqualTo(1);
        // Les valeurs absentes du jeu de donnees sont créées a la volee
        assertThat(modifie.path("dureeConservation").path("valeur").asText()).isEqualTo("20 ans");
        assertThat(modifie.path("responsableTraitement").path("valeur").asText()).isEqualTo("RSSI");

        // Le traitement voisin, qui porte une autre finalite, n'a pas bouge
        assertThat(lire(2).path("finalitePrincipale").path("valeur").asText()).isEqualTo("Analyse commerciale");
    }

    @Test
    void videUneValeurDeReferentiel() throws Exception {
        envoyer(post("/traitements"),
                corpsTraitement(15, "Avec finalite", "A supprimer", "3 ans", "RSSI"), 201);

        String sansReferentiel = """
                {
                  "idFonctionnel": 15,
                  "nom": "Avec finalite",
                  "dateIdentification": "2026-02-01",
                  "client": { "id": "%s" }
                }
                """.formatted(CLIENT_BRETECHE);
        envoyer(put("/traitements/15"), sansReferentiel, 200);

        JsonNode relu = lire(15);
        assertThat(relu.path("finalitePrincipale").isNull()).isTrue();
        assertThat(relu.path("dureeConservation").isNull()).isTrue();
        assertThat(relu.path("responsableTraitement").isNull()).isTrue();
        // La ligne de referentiel reste disponible pour les autres traitements
        assertThat(compterDefinitions("A supprimer")).isEqualTo(1);
    }
}
