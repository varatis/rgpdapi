package com.minds.rgpd.business.utilities.mappers;

import com.minds.rgpd.business.dtos.DefinitionDTO;
import com.minds.rgpd.business.dtos.DureeDTO;
import com.minds.rgpd.business.dtos.ResponsableTraitementDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Traitement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Le referentiel d'un client est partage entre ses traitements : une definition,
 * une duree ou un responsable est pointe par plusieurs traitements a la fois.
 * <p>
 * La mise a jour d'un traitement doit donc <em>remplacer</em> la reference, et
 * surtout pas ecrire dans l'entite pointee : celle-ci est managee par Hibernate,
 * et la modifier renommerait la valeur pour tous les autres traitements.
 */
class TraitementMapperUpdateTest {

    private TraitementMapper traitementMapper;
    private Client client;
    private FinalitePrincipale finaliteExistante;
    private Duree dureeExistante;
    private ResponsableTraitement responsableExistant;
    private Traitement traitement;

    @BeforeEach
    void setUp() throws Exception {
        traitementMapper = new TraitementMapperImpl();
        // TraitementMapperImpl s'appuie sur ClientRefMapper, injecte par Spring en temps normal
        var champ = java.util.Arrays.stream(TraitementMapperImpl.class.getDeclaredFields())
                .filter(f -> f.getType().equals(ClientRefMapper.class))
                .findFirst()
                .orElseThrow();
        champ.setAccessible(true);
        champ.set(traitementMapper, new ClientRefMapperImpl());

        client = Client.builder().id(UUID.randomUUID()).nom("Dupont").statut("ACTIF").build();
        finaliteExistante = FinalitePrincipale.builder()
                .id(1).valeur("Gestion de la paie").client(client).build();
        dureeExistante = Duree.builder()
                .id(2).valeur("5 ans").estArchivage(Duree.CONSERVATION).client(client).build();
        responsableExistant = ResponsableTraitement.builder()
                .id(3).valeur("DSI").informationsComplementaires("Poste 42").client(client).build();

        traitement = Traitement.builder()
                .identifiant(UUID.randomUUID())
                .nom("Paie")
                .client(client)
                .finalitePrincipale(finaliteExistante)
                .dureeConservation(dureeExistante)
                .responsableTraitement(responsableExistant)
                .build();
    }

    private TraitementDTO dtoAvec(String finalite, String duree, String responsable) {
        return TraitementDTO.builder()
                .nom("Paie")
                .finalitePrincipale(new DefinitionDTO(1, FinalitePrincipale.TYPE, finalite))
                .dureeConservation(DureeDTO.builder()
                        .id(2).estArchivage(Duree.CONSERVATION).valeur(duree).build())
                .responsableTraitement(ResponsableTraitementDTO.builder()
                        .id(3).valeur(responsable).build())
                .build();
    }

    /**
     * Reproduit l'enchainement du service : mise a jour des champs simples,
     * puis report des references depuis le traitement transitoire sur lequel
     * travaillent les resolveurs.
     */
    private void appliquer(TraitementDTO traitementDTO) {
        traitementMapper.updateTraitementFromDto(traitementDTO, traitement);
        traitementMapper.copierReferentiels(traitementMapper.mapToTraitement(traitementDTO), traitement);
    }

    @Test
    void nEcritPasDansLeReferentielPartage() {
        appliquer(dtoAvec("Gestion des carrieres", "10 ans", "DRH"));

        assertEquals("Gestion de la paie", finaliteExistante.getValeur());
        assertEquals("5 ans", dureeExistante.getValeur());
        assertEquals("DSI", responsableExistant.getValeur());
    }

    @Test
    void neTouchePasALIdentiteDesEntitesPointees() {
        // Regression : la mise a jour sur place remettait l'id du DTO, quitte a
        // l'annuler, sur une entite managee par Hibernate.
        appliquer(dtoAvec("Gestion des carrieres", "10 ans", "DRH"));

        assertEquals(1, finaliteExistante.getId());
        assertEquals(2, dureeExistante.getId());
        assertEquals(3, responsableExistant.getId());
        assertEquals(client, finaliteExistante.getClient());
        assertEquals("Poste 42", responsableExistant.getInformationsComplementaires());
    }

    @Test
    void remplaceLaReferenceParUneInstanceTransitoire() {
        appliquer(dtoAvec("Gestion des carrieres", "10 ans", "DRH"));

        assertNotSame(finaliteExistante, traitement.getFinalitePrincipale());
        assertEquals("Gestion des carrieres", traitement.getFinalitePrincipale().getValeur());
        assertEquals("10 ans", traitement.getDureeConservation().getValeur());
        assertEquals("DRH", traitement.getResponsableTraitement().getValeur());

        // L'identite et le client sont laisses aux resolveurs
        assertNull(traitement.getFinalitePrincipale().getId());
        assertNull(traitement.getFinalitePrincipale().getClient());
        assertNull(traitement.getDureeConservation().getId());
        assertNull(traitement.getResponsableTraitement().getId());
    }

    @Test
    void videLaReferenceQuandLeDtoNePorteRien() {
        appliquer(TraitementDTO.builder().nom("Paie").build());

        assertNull(traitement.getFinalitePrincipale());
        assertNull(traitement.getDureeConservation());
        assertNull(traitement.getResponsableTraitement());
        // L'entite pointee auparavant reste intacte
        assertEquals("Gestion de la paie", finaliteExistante.getValeur());
    }

    @Test
    void neRecreePasLaReferenceQuandLaValeurEstInchangee() {
        appliquer(dtoAvec("Gestion de la paie", "5 ans", "DSI"));

        assertNotSame(finaliteExistante, traitement.getFinalitePrincipale());
        assertEquals("Gestion de la paie", traitement.getFinalitePrincipale().getValeur());
        // Le resolveur retrouvera l'entite persistee a partir de cette valeur
        assertSame(client, finaliteExistante.getClient());
    }
}
