package com.minds.rgpd.business.utilities;

import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.exceptions.InvalidTraitementException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class TraitementValidatorTest {

    private static final LocalDate IDENTIFICATION = LocalDate.of(2024, 3, 15);

    private final TraitementValidator validator = new TraitementValidator();

    private static TraitementDTO.TraitementDTOBuilder valid() {
        return TraitementDTO.builder()
                .idFonctionnel(1)
                .nom("Gestion RH")
                .client(ClientDTO.builder().id(UUID.randomUUID()).build())
                .dateIdentification(IDENTIFICATION);
    }

    private static Map<String, String> errors(Runnable validation) {
        return catchThrowableOfType(InvalidTraitementException.class, validation::run).getErrors();
    }

    @Test
    void acceptsValidTraitement() {
        assertThatCode(() -> validator.validateCreation(valid().build())).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateCreation(valid().dateMiseAJour(IDENTIFICATION).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateUpdate(valid().dateMiseAJour(IDENTIFICATION.plusDays(1)).build()))
                .doesNotThrowAnyException();
    }

    @Test
    void creationReportsEveryMissingRequiredField() {
        TraitementDTO empty = TraitementDTO.builder().nom("   ").build();

        assertThat(errors(() -> validator.validateCreation(empty))).containsOnlyKeys(
                "idFonctionnel", "nom", "client", "dateIdentification");
    }

    @Test
    void updateDoesNotRequireIdFonctionnelInBody() {
        TraitementDTO withoutId = valid().idFonctionnel(null).build();

        assertThatCode(() -> validator.validateUpdate(withoutId)).doesNotThrowAnyException();
        assertThat(errors(() -> validator.validateUpdate(valid().nom(null).build())))
                .containsExactlyEntriesOf(Map.of("nom", TraitementValidator.MSG_REQUIRED));
    }

    @Test
    void rejectsDateMiseAJourBeforeDateIdentification() {
        TraitementDTO traitement = valid().dateMiseAJour(IDENTIFICATION.minusDays(1)).build();

        assertThat(errors(() -> validator.validateCreation(traitement)))
                .containsExactlyEntriesOf(Map.of("dateMiseAJour", TraitementValidator.MSG_DATE_MISE_A_JOUR));
    }
}
