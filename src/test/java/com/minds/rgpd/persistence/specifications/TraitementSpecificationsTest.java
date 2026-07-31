package com.minds.rgpd.persistence.specifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TraitementSpecificationsTest {

    @Test
    void likePattern_echappePourcentEtUnderscore() {
        // % et _ saisis par l'utilisateur ne doivent pas agir comme wildcards SQL
        assertThat(TraitementSpecifications.likePattern("%")).isEqualTo("%\\%%");
        assertThat(TraitementSpecifications.likePattern("_a")).isEqualTo("%\\_a%");
        assertThat(TraitementSpecifications.likePattern("100%_safe")).isEqualTo("%100\\%\\_safe%");
    }

    @Test
    void likePattern_echappeBackslashAvantLesWildcards() {
        // L'ordre d'échappement du \ est critique pour ne pas double-échapper ensuite
        assertThat(TraitementSpecifications.likePattern("a\\b%c")).isEqualTo("%a\\\\b\\%c%");
    }

    @ParameterizedTest
    @CsvSource({
            "RH, %rh%",
            "Paie, %paie%",
            "GESTION, %gestion%"
    })
    void likePattern_normaliseEnMinusculesEtAjouteWildcardsContient(String input, String expected) {
        assertThat(TraitementSpecifications.likePattern(input)).isEqualTo(expected);
    }

    @Test
    void likeEscapeChar_estBackslash() {
        assertThat(TraitementSpecifications.LIKE_ESCAPE_CHAR).isEqualTo('\\');
    }
}
