package com.minds.rgpd.integrationtest;

import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.identity.IdentiteCommande;
import com.minds.rgpd.identity.FakeIdentityGateway;
import com.minds.rgpd.identity.IdentityGatewayTestConfiguration;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.EtablissementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(IdentityGatewayTestConfiguration.class)
@Sql("classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientSuppressionIT extends AbstractITSpring {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EtablissementRepository etablissementRepository;

    @Autowired
    private FakeIdentityGateway gateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        gateway.reinitialiser();
    }

    @Test
    void laSuppressionDuClientEmporteSonGroupeEtSesUtilisateursKeycloak() throws Exception {
        Client client = clientRepository.findByNom("La breteche").orElseThrow();
        UUID alice = gateway.creerUtilisateur(
                new IdentiteCommande("Alice", "Dupont", "alice@bret.fr", List.of("admin"), "La breteche", true));
        UUID bob = gateway.creerUtilisateur(
                new IdentiteCommande("Bob", "Martin", "bob@bret.fr", List.of("user"), "La breteche", true));
        UUID claire = gateway.creerUtilisateur(
                new IdentiteCommande("Claire", "Durand", "claire@alpha.fr", List.of("user"), "Entreprise Alpha", true));

        mockMvc.perform(delete("/clients/" + client.getId()))
                .andExpect(status().isNoContent());

        assertThat(clientRepository.findById(client.getId())).isEmpty();
        assertThat(gateway.utilisateursSupprimes()).containsExactlyInAnyOrder(alice, bob);
        assertThat(gateway.groupesSupprimes()).containsExactly("La breteche");
        assertThat(gateway.utilisateur(claire)).isPresent();
    }

    @Test
    void laSuppressionDunClientSansUtilisateurNeBloquePas() throws Exception {
        Client client = clientRepository.findByNom("Entreprise Beta").orElseThrow();

        mockMvc.perform(delete("/clients/" + client.getId()))
                .andExpect(status().isNoContent());

        assertThat(clientRepository.findById(client.getId())).isEmpty();
        assertThat(gateway.utilisateursSupprimes()).isEmpty();
    }

    @Test
    void laSuppressionDuClientEmporteLesDonneesRattachees() throws Exception {
        Client client = clientRepository.findByNom("La breteche").orElseThrow();
        assertThat(etablissementRepository.findByNom("Siège Paris")).isPresent();

        mockMvc.perform(delete("/clients/" + client.getId()))
                .andExpect(status().isNoContent());

        assertThat(etablissementRepository.findByNom("Siège Paris")).isEmpty();
        assertThat(etablissementRepository.findByNom("Siège Marseille")).isPresent();
    }

    @Test
    void unClientInconnuRenvoitUneErreur404() throws Exception {
        mockMvc.perform(delete("/clients/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void laSuppressionEstRefuseeSiLeNomDuClientNestPasUnUuid() throws Exception {
        mockMvc.perform(delete("/clients/la-breteche"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laListeDesClientsNeContientPlusLeClientSupprime() throws Exception {
        Client client = clientRepository.findByNom("La breteche").orElseThrow();
        mockMvc.perform(delete("/clients/" + client.getId())).andExpect(status().isNoContent());

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
