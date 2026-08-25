package com.minds.rgpd.business.utilities;

import com.minds.rgpd.annotation.DataJpaTestWithTestContainers;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.DureeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTestWithTestContainers
@Import(DureeResolver.class)
class DureeResolverTest {

    @Autowired
    private DureeResolver dureeResolver;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DureeRepository dureeRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(Client.builder().nom("Dupont").statut("ACTIF").build());
    }

    private Traitement traitementAvec(String conservation, String archivage) {
        return Traitement.builder()
                .dureeConservation(Duree.builder().valeur(conservation).build())
                .dureeArchivage(Duree.builder().valeur(archivage).build())
                .build();
    }

    @Test
    void creeLaDureeQuandElleNexistePas() {
        Traitement traitement = traitementAvec("5 ans", "10 ans");

        dureeResolver.resolveDurees(traitement, client);

        assertNotNull(traitement.getDureeConservation().getId());
        assertEquals("5 ans", traitement.getDureeConservation().getValeur());
        assertFalse(traitement.getDureeConservation().isEstArchivage());
        assertEquals(client.getId(), traitement.getDureeConservation().getClient().getId());
        assertTrue(traitement.getDureeArchivage().isEstArchivage());
        assertEquals(2, dureeRepository.count());
    }

    @Test
    void reutiliseLaDureeExistantePourLaMemeValeur() {
        Traitement premier = traitementAvec("5 ans", "10 ans");
        dureeResolver.resolveDurees(premier, client);
        Integer idAttendu = premier.getDureeConservation().getId();

        // Deuxieme ligne du meme import, meme valeur : aucune duree en plus
        Traitement second = traitementAvec("5 ans", "10 ans");
        dureeResolver.resolveDurees(second, client);

        assertEquals(idAttendu, second.getDureeConservation().getId());
        assertEquals(2, dureeRepository.count());
    }

    @Test
    void neConfondPasConservationEtArchivagePourLaMemeValeur() {
        Traitement traitement = traitementAvec("5 ans", "5 ans");

        dureeResolver.resolveDurees(traitement, client);

        assertEquals(2, dureeRepository.count());
        assertNotNull(traitement.getDureeConservation().getId());
        assertNotNull(traitement.getDureeArchivage().getId());
        assertFalse(traitement.getDureeConservation().isEstArchivage());
        assertTrue(traitement.getDureeArchivage().isEstArchivage());
    }

    @Test
    void separeLesDureesParClient() {
        Traitement pourDupont = traitementAvec("5 ans", "10 ans");
        dureeResolver.resolveDurees(pourDupont, client);

        Client autreClient = clientRepository.save(Client.builder().nom("Martin").statut("ACTIF").build());
        Traitement pourMartin = traitementAvec("5 ans", "10 ans");
        dureeResolver.resolveDurees(pourMartin, autreClient);

        assertEquals(4, dureeRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurVide() {
        Traitement traitement = traitementAvec("   ", null);
        traitement.setDureeArchivage(null);

        dureeResolver.resolveDurees(traitement, client);

        assertNull(traitement.getDureeConservation());
        assertNull(traitement.getDureeArchivage());
        assertEquals(0, dureeRepository.count());
    }

    // Entree par la valeur brute : chemin emprunte par l'import Excel, ou la
    // colonne ne fournit qu'une chaine et non une duree deja construite.

    @Test
    void creeLaDureeDepuisUneValeurBrute() {
        Duree conservation = dureeResolver.resolveDureeConservation("  5 ans  ", client);
        Duree archivage = dureeResolver.resolveDureeArchivage("10 ans", client);

        assertNotNull(conservation.getId());
        assertEquals("5 ans", conservation.getValeur());
        assertFalse(conservation.isEstArchivage());
        assertEquals(client.getId(), conservation.getClient().getId());
        assertTrue(archivage.isEstArchivage());
        assertEquals(2, dureeRepository.count());
    }

    @Test
    void reutiliseLaDureeExistantePourUneValeurBrute() {
        // Deux lignes du meme import citant la meme duree
        Duree premiere = dureeResolver.resolveDureeConservation("5 ans", client);
        Duree seconde = dureeResolver.resolveDureeConservation("5 ans", client);

        assertEquals(premiere.getId(), seconde.getId());
        assertEquals(1, dureeRepository.count());
    }

    @Test
    void neConfondPasConservationEtArchivagePourLaMemeValeurBrute() {
        dureeResolver.resolveDureeConservation("5 ans", client);
        dureeResolver.resolveDureeArchivage("5 ans", client);

        assertEquals(2, dureeRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurBruteVide() {
        assertNull(dureeResolver.resolveDureeConservation("   ", client));
        assertNull(dureeResolver.resolveDureeArchivage(null, client));
        assertEquals(0, dureeRepository.count());
    }
}
