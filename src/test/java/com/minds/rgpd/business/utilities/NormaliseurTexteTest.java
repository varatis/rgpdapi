package com.minds.rgpd.business.utilities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormaliseurTexteTest {

    @Test
    void normalisationEnMinusculesSansAccents() {
        assertThat(NormaliseurTexte.normaliser("  La BrÈteche  ")).isEqualTo("la breteche");
        assertThat(NormaliseurTexte.normaliser("Ondé")).isEqualTo("onde");
        assertThat(NormaliseurTexte.normaliser("Ægir")).isEqualTo("ægir");
    }

    @Test
    void normalisationDunVide() {
        assertThat(NormaliseurTexte.normaliser(null)).isNull();
        assertThat(NormaliseurTexte.normaliser("   ")).isEmpty();
    }

    @Test
    void unFiltreVideLaisseToutPasser() {
        assertThat(NormaliseurTexte.contient("Dupont", null)).isTrue();
        assertThat(NormaliseurTexte.contient("Dupont", "  ")).isTrue();
        assertThat(NormaliseurTexte.contient(null, null)).isTrue();
    }

    @Test
    void uneValeurNulleEstRefuseeDesQuunFiltreEstPose() {
        assertThat(NormaliseurTexte.contient(null, "dup")).isFalse();
    }

    @Test
    void laCorrespondanceEstPartielleEtIndifferenteACasseEtAuxAccents() {
        assertThat(NormaliseurTexte.contient("Éliane Dufour", "elian")).isTrue();
        assertThat(NormaliseurTexte.contient("Eliane Dufour", "ÉLIAN")).isTrue();
        assertThat(NormaliseurTexte.contient("Eliane Dufour", "dufore")).isFalse();
    }
}
