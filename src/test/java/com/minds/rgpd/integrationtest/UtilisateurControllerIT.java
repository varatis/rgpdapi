package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.identity.FakeIdentityGateway;
import com.minds.rgpd.identity.IdentityGatewayTestConfiguration;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(IdentityGatewayTestConfiguration.class)
@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtilisateurControllerIT extends AbstractITSpring {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FakeIdentityGateway gateway;

    @Autowired
    private ClientRepository clientRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        gateway.reinitialiser();
    }

    @Test
    void laListeVientDeKeycloak() throws Exception {
        creer("Alice", "Dupont", "alice@exemple.fr", List.of("admin"), null);
        creer("Bob", "Martin", "bob@exemple.fr", List.of("user"), null);

        mockMvc.perform(get("/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nom").value("Dupont"))
                .andExpect(jsonPath("$.content[1].nom").value("Martin"));
    }

    @Test
    void leFiltreSurLeNomEstPartielEtSansCasse() throws Exception {
        creer("Alice", "Dupré", "a@exemple.fr", List.of("user"), null);
        creer("Bob", "Martin", "b@exemple.fr", List.of("user"), null);

        mockMvc.perform(get("/utilisateurs").param("nom", "DUPR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].prenom").value("Alice"));
    }

    @Test
    void leFiltreSurLePrenom() throws Exception {
        creer("Alice", "Dupont", "a@exemple.fr", List.of("user"), null);
        creer("Aliénor", "Martin", "b@exemple.fr", List.of("user"), null);

        mockMvc.perform(get("/utilisateurs").param("prenom", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].prenom").value("Alice"))
                .andExpect(jsonPath("$.content[1].prenom").value("Aliénor"));
    }

    @Test
    void leFiltreSurLeClientRestreintAuxMembresDuGroupe() throws Exception {
        UUID idClient = client("La breteche").getId();
        creer("Alice", "Dupont", "alice@bret.fr", List.of("admin"), "La breteche");
        creer("Bob", "Martin", "bob@alpha.fr", List.of("user"), "Entreprise Alpha");

        mockMvc.perform(get("/utilisateurs").param("clientId", idClient.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@bret.fr"))
                .andExpect(jsonPath("$.content[0].clientId").value(idClient.toString()))
                .andExpect(jsonPath("$.content[0].clientNom").value("La breteche"));
    }

    @Test
    void leFiltreCombineNomEtClient() throws Exception {
        UUID idClient = client("La breteche").getId();
        creer("Alice", "Dupont", "alice@bret.fr", List.of("admin"), "La breteche");
        creer("Anna", "Dupont", "anna@alpha.fr", List.of("user"), "Entreprise Alpha");

        mockMvc.perform(get("/utilisateurs")
                        .param("clientId", idClient.toString())
                        .param("nom", "dup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@bret.fr"));
    }

    @Test
    void laRechercheEstPagineeEtTriee() throws Exception {
        for (int index = 0; index < 5; index++) {
            creer("Prénom" + index, "Nom" + index, "u" + index + "@exemple.fr", List.of("user"), null);
        }

        mockMvc.perform(get("/utilisateurs").param("page", "1").param("size", "2").param("sort", "nom,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].nom").value("Nom2"));
    }

    @Test
    void laCreationEstRepercuteeDansKeycloak() throws Exception {
        UUID idClient = client("La breteche").getId();

        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", "alice@nouveau.fr", List.of("ADMIN"), idClient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@nouveau.fr"))
                .andExpect(jsonPath("$.clientNom").value("La breteche"))
                .andExpect(jsonPath("$.roles[0]").value("admin"));

        assertThat(gateway.nombreUtilisateurs()).isEqualTo(1);
        IdentiteCommande recue = gateway.commandesRecues().getFirst();
        assertThat(recue.groupe()).isEqualTo("La breteche");
        assertThat(recue.roles()).containsExactly("admin");
        assertThat(recue.actif()).isTrue();
    }

    @Test
    void laCreationSansClientEstAcceptee() throws Exception {
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Bob", "Martin", "bob@libre.fr", List.of("user"), null)))
                .andExpect(status().isCreated());

        assertThat(gateway.commandesRecues().getFirst().groupe()).isNull();
    }

    @Test
    void laCreationRefuseUnRoleInconnu() throws Exception {
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", "alice@exemple.fr", List.of("superheros"), null)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(Matchers.containsString("superheros")));

        assertThat(gateway.nombreUtilisateurs()).isZero();
    }

    @Test
    void laCreationRefuseUnEmailManquant() throws Exception {
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", null, List.of("admin"), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"));

        assertThat(gateway.nombreUtilisateurs()).isZero();
    }

    @Test
    void laCreationRefuseUneListeDeRolesVide() throws Exception {
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", "alice@exemple.fr", List.of(), null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laCreationRefuseUnClientInconnu() throws Exception {
        mockMvc.perform(post("/utilisateurs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", "alice@exemple.fr", List.of("admin"), UUID.randomUUID())))
                .andExpect(status().isNotFound());

        assertThat(gateway.nombreUtilisateurs()).isZero();
    }

    @Test
    void laModificationMetAJourLidentite() throws Exception {
        UUID id = creer("Alice", "Dupont", "alice@exemple.fr", List.of("user"), "La breteche");

        mockMvc.perform(put("/utilisateurs/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alicia", "Dupont", "alicia@exemple.fr", List.of("admin", "user"), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Alicia"))
                .andExpect(jsonPath("$.email").value("alicia@exemple.fr"))
                .andExpect(jsonPath("$.roles.length()").value(2));

        assertThat(gateway.utilisateur(id).orElseThrow().prenom()).isEqualTo("Alicia");
    }

    @Test
    void laModificationDunUtilisateurInconnuRenvoit404() throws Exception {
        mockMvc.perform(put("/utilisateurs/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps("Alice", "Dupont", "alice@exemple.fr", List.of("admin"), null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void laSuppressionRetireLutilisateurDeKeycloak() throws Exception {
        UUID id = creer("Alice", "Dupont", "alice@exemple.fr", List.of("user"), null);

        mockMvc.perform(delete("/utilisateurs/" + id))
                .andExpect(status().isNoContent());

        assertThat(gateway.nombreUtilisateurs()).isZero();
        assertThat(gateway.utilisateursSupprimes()).containsExactly(id);
    }

    @Test
    void laSuppressionDunUtilisateurInconnuRenvoit404() throws Exception {
        mockMvc.perform(delete("/utilisateurs/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void leDetailDunUtilisateur() throws Exception {
        UUID id = creer("Alice", "Dupont", "alice@exemple.fr", List.of("admin"), "La breteche");

        mockMvc.perform(get("/utilisateurs/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.roles[0]").value("admin"))
                .andExpect(jsonPath("$.clientNom").value("La breteche"));
    }

    @Test
    void lesRolesProposesViennentDeKeycloak() throws Exception {
        mockMvc.perform(get("/utilisateurs/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("admin"))
                .andExpect(jsonPath("$[1]").value("user"));
    }

    private UUID creer(String prenom, String nom, String email, List<String> roles, String groupe) {
        return gateway.creerUtilisateur(new IdentiteCommande(prenom, nom, email, roles, groupe, true));
    }

    private Client client(String nom) {
        return clientRepository.findByNom(nom).orElseThrow();
    }

    private String corps(String prenom, String nom, String email, List<String> roles, UUID clientId) throws Exception {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("prenom", prenom);
        corps.put("nom", nom);
        corps.put("email", email);
        corps.put("roles", roles);
        if (clientId != null) {
            corps.put("clientId", clientId);
        }
        return mapper.writeValueAsString(corps);
    }
}
