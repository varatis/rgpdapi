package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.entities.FinalitePrincipale;
import com.minds.rgpd.persistence.entities.Traitement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraitementDiffTest {

    private Traitement traitement() {
        Client client = Client.builder().id(UUID.randomUUID()).nom("La breteche").build();
        return Traitement.builder()
                .identifiant(UUID.randomUUID())
                .idFonctionnel(1)
                .nom("Paie")
                .client(client)
                .dateIdentification(LocalDate.of(2024, 1, 1))
                .finalitePrincipale(FinalitePrincipale.builder().id(1).valeur("Gestion RH").client(client).build())
                .dureeConservation(Duree.builder().id(1).valeur("5 ans").estArchivage(Duree.CONSERVATION).build())
                .etablissements(List.of(Etablissement.builder().id(UUID.randomUUID()).nom("Siège").build()))
                .build();
    }

    @Test
    void aucunEcart_donneUnMotifNul() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        assertThat(TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement))).isNull();
    }

    @Test
    void unChampModifie_apparaitDansLeMotif() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitement.setNom("Paie et primes");

        String motif = TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement));
        assertThat(motif).contains("nom").contains("Paie").contains("Paie et primes");
    }

    @Test
    void referentielModifie_estCompareSurSaValeur() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitement.setFinalitePrincipale(
                FinalitePrincipale.builder().id(2).valeur("Gestion de la paie").build());

        String motif = TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement));
        assertThat(motif).contains("finalitePrincipale").contains("Gestion RH").contains("Gestion de la paie");
    }

    @Test
    void dateDeMiseAJour_estExclueDuMotif() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitement.setDateMiseAJour(LocalDate.now());

        assertThat(TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement))).isNull();
    }

    @Test
    void colonneComplementaireModifiee_estHistorisee() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitement.setScoreGlobal(12);

        assertThat(TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement)))
                .contains("scoreGlobal").contains("12");
    }

    @Test
    void valeurLongue_estAbregeePourResterLisible() {
        Traitement traitement = traitement();
        Map<String, String> avant = TraitementDiff.snapshot(traitement);

        traitement.setCommentaires("x".repeat(500));

        String motif = TraitementDiff.motifDeModification(avant, TraitementDiff.snapshot(traitement));
        assertThat(motif).contains("...");
        assertThat(motif.length()).isLessThan(300);
    }
}
