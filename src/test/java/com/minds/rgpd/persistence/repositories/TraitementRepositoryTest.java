package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.annotation.DataJpaTestWithTestContainers;
import com.minds.rgpd.business.utilities.DefinitionResolver;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.Traitement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTestWithTestContainers
// Meme configuration de contexte que DefinitionResolverTest : le contexte (et
// donc le conteneur PostgreSQL) est mis en cache et partage entre les deux.
@Import(DefinitionResolver.class)
class TraitementRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    @Autowired
    private TraitementRepository traitementRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DefinitionRepository definitionRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(Client.builder().nom("Dupont").statut("ACTIF").build());
    }

    private Traitement save(String nom, String gestionnaire, String finalite, int idFonctionnel) {
        FinalitePrincipale definition = null;
        if (finalite != null) {
            definition = definitionRepository.save(
                    FinalitePrincipale.builder().valeur(finalite).client(client).build());
        }
        return traitementRepository.save(Traitement.builder()
                .idFonctionnel(idFonctionnel)
                .nom(nom)
                .gestionnaireMiseEnOeuvre(gestionnaire)
                .finalitePrincipale(definition)
                .client(client)
                .dateIdentification(DATE)
                .build());
    }

    @Test
    void retrouveLeDoublonQuandLaFinaliteEstRenseignee() {
        save("Paie", "RH", "Gestion de la paie", 1);

        List<Traitement> trouves = traitementRepository.findByAllBusinessFields(
                "Paie", client, "RH", "Gestion de la paie", DATE);

        assertEquals(1, trouves.size());
    }

    @Test
    void retrouveLeDoublonQuandLaFinaliteEstAbsente() {
        // Regression : un traitement sans finalite doit rester detectable comme
        // doublon, sinon il est reimporte a chaque envoi de fichier.
        save("Paie", "RH", null, 2);

        List<Traitement> trouves = traitementRepository.findByAllBusinessFields(
                "Paie", client, "RH", null, DATE);

        assertEquals(1, trouves.size());
    }

    @Test
    void neConfondPasUneFinaliteAbsenteAvecUneFinaliteRenseignee() {
        save("Paie", "RH", "Gestion de la paie", 3);

        List<Traitement> trouves = traitementRepository.findByAllBusinessFields(
                "Paie", client, "RH", null, DATE);

        assertTrue(trouves.isEmpty());
    }

    @Test
    void neRetrouveRienSiLaFinaliteDiffere() {
        save("Paie", "RH", "Gestion de la paie", 4);

        List<Traitement> trouves = traitementRepository.findByAllBusinessFields(
                "Paie", client, "RH", "Autre finalite", DATE);

        assertTrue(trouves.isEmpty());
    }
}
