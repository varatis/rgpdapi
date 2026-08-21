package com.minds.rgpd.persistence.specifications;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PreconisationSpecificationsTest {

    @Test
    void likePattern_echappePourcentEtUnderscore() {
        assertThat(PreconisationSpecifications.likePattern("%")).isEqualTo("%\\%%");
        assertThat(PreconisationSpecifications.likePattern("_a")).isEqualTo("%\\_a%");
        assertThat(PreconisationSpecifications.likePattern("100%_safe")).isEqualTo("%100\\%\\_safe%");
    }

    @ParameterizedTest
    @CsvSource({
            "DPO, %dpo%",
            "Broyeur, %broyeur%",
            "BUREAUX, %bureaux%"
    })
    void likePattern_normaliseEnMinusculesEtAjouteWildcardsContient(String input, String expected) {
        assertThat(PreconisationSpecifications.likePattern(input)).isEqualTo(expected);
    }
}
