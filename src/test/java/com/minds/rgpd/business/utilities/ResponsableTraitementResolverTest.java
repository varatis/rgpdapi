package com.minds.rgpd.business.utilities;

import com.minds.rgpd.annotation.DataJpaTestWithTestContainers;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.ResponsableTraitementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTestWithTestContainers
@Import(ResponsableTraitementResolver.class)
class ResponsableTraitementResolverTest {

    @Autowired
    private ResponsableTraitementResolver responsableTraitementResolver;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ResponsableTraitementRepository responsableTraitementRepository;

    private Client client;

    @BeforeEach
    void setUp() {
        client = clientRepository.save(Client.builder().nom("Dupont").statut("ACTIF").build());
    }

    private Traitement traitementAvec(String responsable) {
        return Traitement.builder()
                .responsableTraitement(ResponsableTraitement.builder().valeur(responsable).build())
                .build();
    }

    @Test
    void creeLeResponsableQuandIlNexistePas() {
        Traitement traitement = traitementAvec("Direction des systemes d'information");

        responsableTraitementResolver.resolveResponsableTraitement(traitement, client);

        assertNotNull(traitement.getResponsableTraitement().getId());
        assertEquals("Direction des systemes d'information", traitement.getResponsableTraitement().getValeur());
        assertEquals(client.getId(), traitement.getResponsableTraitement().getClient().getId());
        assertEquals(1, responsableTraitementRepository.count());
    }

    @Test
    void laisseLesInformationsComplementairesNullesALImport() {
        Traitement traitement = traitementAvec("Direction des systemes d'information");

        responsableTraitementResolver.resolveResponsableTraitement(traitement, client);

        assertNull(traitement.getResponsableTraitement().getInformationsComplementaires());
    }

    @Test
    void reutiliseLeResponsableExistantPourLaMemeValeur() {
        Traitement premier = traitementAvec("Direction des systemes d'information");
        responsableTraitementResolver.resolveResponsableTraitement(premier, client);
        Integer idAttendu = premier.getResponsableTraitement().getId();

        // Deuxieme ligne du meme import, meme valeur : aucun responsable en plus
        Traitement second = traitementAvec("Direction des systemes d'information");
        responsableTraitementResolver.resolveResponsableTraitement(second, client);

        assertEquals(idAttendu, second.getResponsableTraitement().getId());
        assertEquals(1, responsableTraitementRepository.count());
    }

    @Test
    void acceptePlusieursResponsablesPourUnMemeClient() {
        Traitement premier = traitementAvec("Direction des systemes d'information");
        responsableTraitementResolver.resolveResponsableTraitement(premier, client);

        Traitement second = traitementAvec("Direction des ressources humaines");
        responsableTraitementResolver.resolveResponsableTraitement(second, client);

        assertEquals(2, responsableTraitementRepository.count());
    }

    @Test
    void separeLesResponsablesParClient() {
        Traitement pourDupont = traitementAvec("Direction des systemes d'information");
        responsableTraitementResolver.resolveResponsableTraitement(pourDupont, client);

        Client autreClient = clientRepository.save(Client.builder().nom("Martin").statut("ACTIF").build());
        Traitement pourMartin = traitementAvec("Direction des systemes d'information");
        responsableTraitementResolver.resolveResponsableTraitement(pourMartin, autreClient);

        assertEquals(2, responsableTraitementRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurVide() {
        Traitement traitement = traitementAvec("   ");

        responsableTraitementResolver.resolveResponsableTraitement(traitement, client);

        assertNull(traitement.getResponsableTraitement());
        assertEquals(0, responsableTraitementRepository.count());
    }

    // Entree par la valeur brute : chemin emprunte par l'import Excel, ou la
    // colonne ne fournit qu'une chaine et non un responsable deja construit.

    @Test
    void creeLeResponsableDepuisUneValeurBrute() {
        ResponsableTraitement responsable = responsableTraitementResolver
                .resolveResponsableTraitement("  Direction des systemes d'information  ", client);

        assertNotNull(responsable.getId());
        assertEquals("Direction des systemes d'information", responsable.getValeur());
        assertNull(responsable.getInformationsComplementaires());
        assertEquals(client.getId(), responsable.getClient().getId());
        assertEquals(1, responsableTraitementRepository.count());
    }

    @Test
    void reutiliseLeResponsableExistantPourUneValeurBrute() {
        // Deux lignes du meme import citant le meme responsable
        ResponsableTraitement premier = responsableTraitementResolver
                .resolveResponsableTraitement("Direction des systemes d'information", client);
        ResponsableTraitement second = responsableTraitementResolver
                .resolveResponsableTraitement("Direction des systemes d'information", client);

        assertEquals(premier.getId(), second.getId());
        assertEquals(1, responsableTraitementRepository.count());
    }

    @Test
    void laisseLaReferenceNullePourUneValeurBruteVide() {
        assertNull(responsableTraitementResolver.resolveResponsableTraitement("   ", client));
        assertEquals(0, responsableTraitementRepository.count());
    }
}
