package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.annotation.DataJpaTestWithTestContainers;
import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import com.minds.rgpd.business.utilities.DefinitionResolver;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.specifications.EtablissementSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTestWithTestContainers
// Meme configuration de contexte que TraitementRepositoryTest : le contexte (et
// donc le conteneur PostgreSQL) est mis en cache et partage entre les tests.
@Import(DefinitionResolver.class)
class EtablissementRepositoryTest {

    @Autowired
    private EtablissementRepository etablissementRepository;

    @Autowired
    private TraitementRepository traitementRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;
    private Client autreClient;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(Client.builder().nom("Dupont").statut("ACTIF").build());
        autreClient = clientRepository.save(Client.builder().nom("Martin").statut("ACTIF").build());
    }

    private Etablissement save(String nom, String departement, boolean principal, Client proprietaire) {
        // L'identifiant n'est pas genere par la base : le service l'attribue lui aussi.
        return etablissementRepository.save(Etablissement.builder()
                .id(UUID.randomUUID())
                .nom(nom)
                .departement(departement)
                .principal(principal)
                .client(proprietaire)
                .build());
    }

    @Test
    void enregistreLeDepartementEtLIndicateurPrincipal() {
        Etablissement enregistre = save("Ajaccio", "2A", true, client);

        Etablissement relu = etablissementRepository.findById(enregistre.getId()).orElseThrow();

        assertEquals("2A", relu.getDepartement());
        assertTrue(relu.isPrincipal());
    }

    private List<String> chercher(EtablissementFilterCriteria criteria, Pageable pageable) {
        return etablissementRepository
                .findAll(EtablissementSpecifications.search("Dupont", criteria), pageable)
                .getContent().stream().map(Etablissement::getNom).toList();
    }

    @Test
    void listeLaPageDesEtablissementsDUnClientParOrdreAlphabetique() {
        save("Toulouse", "31", false, client);
        save("Ajaccio", "2A", true, client);
        save("Cayenne", "973", false, autreClient);

        Page<Etablissement> trouves = etablissementRepository.findAll(
                EtablissementSpecifications.search("Dupont", null), PageRequest.of(0, 20, Sort.by("nom")));

        assertEquals(2, trouves.getTotalElements());
        assertEquals(List.of("Ajaccio", "Toulouse"), trouves.getContent().stream().map(Etablissement::getNom).toList());
    }

    @Test
    void renvoieUneSeuleLigneParPageEtCompteLeTotal() {
        save("Toulouse", "31", false, client);
        save("Ajaccio", "2A", true, client);

        Page<Etablissement> premierePage = etablissementRepository.findAll(
                EtablissementSpecifications.search("Dupont", null), PageRequest.of(0, 1, Sort.by("nom")));

        assertEquals(2, premierePage.getTotalElements());
        assertEquals(2, premierePage.getTotalPages());
        assertEquals(List.of("Ajaccio"), premierePage.getContent().stream().map(Etablissement::getNom).toList());
    }

    @Test
    void filtreSurUnFragmentDeNomSansTenirCompteDeLaCasse() {
        save("Agence Lyon", "69", false, client);
        save("Siege Paris", "75", true, client);

        assertEquals(List.of("Agence Lyon"),
                chercher(new EtablissementFilterCriteria("lyo", null, null), PageRequest.of(0, 20, Sort.by("nom"))));
    }

    @Test
    void filtreSurLeDepartementEnEgaliteStricte() {
        save("Ajaccio", "2A", true, client);
        save("Bastia", "2B", false, client);

        assertEquals(List.of("Ajaccio"),
                chercher(new EtablissementFilterCriteria(null, "2a", null), PageRequest.of(0, 20, Sort.by("nom"))));
        // "2" ne doit pas remonter les codes corses.
        assertEquals(List.of(),
                chercher(new EtablissementFilterCriteria(null, "2", null), PageRequest.of(0, 20, Sort.by("nom"))));
    }

    @Test
    void filtreSurLEtablissementPrincipal() {
        save("Ajaccio", "2A", true, client);
        save("Toulouse", "31", false, client);

        Pageable pageable = PageRequest.of(0, 20, Sort.by("nom"));
        assertEquals(List.of("Ajaccio"), chercher(new EtablissementFilterCriteria(null, null, true), pageable));
        assertEquals(List.of("Toulouse"), chercher(new EtablissementFilterCriteria(null, null, false), pageable));
    }

    @Test
    void combineLesFiltresEtNeSortJamaisDuClient() {
        save("Agence Lyon", "69", true, client);
        save("Agence Lille", "59", false, client);
        save("Agence Lyon", "69", true, autreClient);

        assertEquals(List.of("Agence Lyon"),
                chercher(new EtablissementFilterCriteria("agence", "69", true), PageRequest.of(0, 20, Sort.by("nom"))));
    }

    @Test
    void detecteUnNomDejaPrisChezLeClientSansTenirCompteDeLaCasse() {
        Etablissement ajaccio = save("Ajaccio", "2A", true, client);

        assertTrue(etablissementRepository.existsByNomIgnoreCaseAndClient("ajaccio", client));
        assertFalse(etablissementRepository.existsByNomIgnoreCaseAndClient("ajaccio", autreClient));
        // Une modification qui garde son propre nom ne doit pas etre vue comme un doublon.
        assertFalse(etablissementRepository.existsByNomIgnoreCaseAndClientAndIdNot("ajaccio", client, ajaccio.getId()));
    }

    @Test
    void compteLesTraitementsRattachesAUnEtablissement() {
        Etablissement ajaccio = save("Ajaccio", "2A", true, client);
        Etablissement toulouse = save("Toulouse", "31", false, client);

        traitementRepository.save(Traitement.builder()
                .idFonctionnel(1)
                .nom("Paie")
                .client(client)
                .dateIdentification(LocalDate.of(2026, 1, 15))
                .etablissements(List.of(ajaccio))
                .build());

        assertEquals(1, traitementRepository.countByEtablissementsContains(ajaccio));
        assertEquals(0, traitementRepository.countByEtablissementsContains(toulouse));
    }
}
