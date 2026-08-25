package com.minds.rgpd.business.utilities;

import com.minds.rgpd.annotation.DataJpaTestWithTestContainers;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.Sensibilite;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.DefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DataJpaTestWithTestContainers
@Import(DefinitionResolver.class)
class DefinitionResolverTest {

    @Autowired
    private DefinitionResolver definitionResolver;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DefinitionRepository definitionRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(Client.builder().nom("Dupont").statut("ACTIF").build());
    }

    private Traitement traitementAvec(String finalite, String sensibilite) {
        return Traitement.builder()
                .finalitePrincipale(FinalitePrincipale.builder().valeur(finalite).build())
                .sensibilite(Sensibilite.builder().valeur(sensibilite).build())
                .build();
    }

    @Test
    void creeLaDefinitionQuandElleNexistePas() {
        Traitement traitement = traitementAvec("Gestion de la paie", "NIR (N SS)");

        definitionResolver.resolveDefinitions(traitement, client);

        assertNotNull(traitement.getFinalitePrincipale().getId());
        assertEquals("Gestion de la paie", traitement.getFinalitePrincipale().getValeur());
        assertEquals(FinalitePrincipale.TYPE, traitement.getFinalitePrincipale().getType());
        assertEquals(client.getId(), traitement.getFinalitePrincipale().getClient().getId());
        assertEquals(Sensibilite.TYPE, traitement.getSensibilite().getType());
        assertEquals(2, definitionRepository.count());
    }

    @Test
    void reutiliseLaDefinitionExistantePourLaMemeValeur() {
        Traitement premier = traitementAvec("Gestion de la paie", "NIR (N SS)");
        definitionResolver.resolveDefinitions(premier, client);
        Integer idAttendu = premier.getFinalitePrincipale().getId();

        // Deuxieme ligne du meme import, meme valeur : aucune definition en plus
        Traitement second = traitementAvec("Gestion de la paie", "NIR (N SS)");
        definitionResolver.resolveDefinitions(second, client);

        assertEquals(idAttendu, second.getFinalitePrincipale().getId());
        assertEquals(2, definitionRepository.count());
    }

    @Test
    void neConfondPasDeuxTypesPortantLaMemeValeur() {
        Traitement traitement = traitementAvec("Valeur commune", "Valeur commune");

        definitionResolver.resolveDefinitions(traitement, client);

        assertEquals(2, definitionRepository.count());
        assertNotNull(traitement.getFinalitePrincipale().getId());
        assertNotNull(traitement.getSensibilite().getId());
    }

    @Test
    void nesepareLesDefinitionsParClient() {
        Traitement pourDupont = traitementAvec("Gestion de la paie", "NIR (N SS)");
        definitionResolver.resolveDefinitions(pourDupont, client);

        Client autreClient = clientRepository.save(Client.builder().nom("Martin").statut("ACTIF").build());
        Traitement pourMartin = traitementAvec("Gestion de la paie", "NIR (N SS)");
        definitionResolver.resolveDefinitions(pourMartin, autreClient);

        assertEquals(4, definitionRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurVide() {
        Traitement traitement = traitementAvec("   ", null);
        traitement.setSensibilite(null);

        definitionResolver.resolveDefinitions(traitement, client);

        assertNull(traitement.getFinalitePrincipale());
        assertNull(traitement.getSensibilite());
        assertEquals(0, definitionRepository.count());
    }

    // Entree par la valeur brute : chemin emprunte par l'import Excel, ou la
    // colonne ne fournit qu'une chaine et non une definition deja construite.

    @Test
    void creeLaDefinitionDepuisUneValeurBrute() {
        FinalitePrincipale finalite = definitionResolver.resolveFinalitePrincipale("  Gestion de la paie  ", client);

        assertNotNull(finalite.getId());
        assertEquals("Gestion de la paie", finalite.getValeur());
        assertEquals(FinalitePrincipale.TYPE, finalite.getType());
        assertEquals(client.getId(), finalite.getClient().getId());
        assertEquals(1, definitionRepository.count());
    }

    @Test
    void reutiliseLaDefinitionExistantePourUneValeurBrute() {
        // Deux lignes du meme import citant la meme finalite
        FinalitePrincipale premiere = definitionResolver.resolveFinalitePrincipale("Gestion de la paie", client);
        FinalitePrincipale seconde = definitionResolver.resolveFinalitePrincipale("Gestion de la paie", client);

        assertEquals(premiere.getId(), seconde.getId());
        assertEquals(1, definitionRepository.count());
    }

    @Test
    void neConfondPasDeuxTypesPortantLaMemeValeurBrute() {
        definitionResolver.resolveFinalitePrincipale("Interne", client);
        definitionResolver.resolveSensibilite("Interne", client);

        assertEquals(2, definitionRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurBruteVide() {
        assertNull(definitionResolver.resolveFinalitePrincipale("   ", client));
        assertNull(definitionResolver.resolveSensibilite(null, client));
        assertEquals(0, definitionRepository.count());
    }
}
