package com.minds.rgpd.business.utilities;

import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.exceptions.InvalidTraitementException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class TraitementValidator {

    static final String MSG_REQUIRED = "Ce champ est requis.";
    static final String MSG_DATE_MISE_A_JOUR =
            "La date de mise à jour doit être postérieure ou égale à la date d'identification du traitement.";

    /**
     * Création : l'identifiant fonctionnel est porté par le corps de la requête.
     *
     * @throws InvalidTraitementException si au moins une règle n'est pas respectée
     */
    public void validateCreation(TraitementDTO traitement) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (Objects.isNull(traitement.idFonctionnel())) {
            errors.put("idFonctionnel", MSG_REQUIRED);
        }
        checkRules(traitement, errors);
        throwIfAny(errors);
    }

    /**
     * Modification : l'identifiant fonctionnel vient de l'URL, celui du corps est ignoré.
     *
     * @throws InvalidTraitementException si au moins une règle n'est pas respectée
     */
    public void validateUpdate(TraitementDTO traitement) {
        Map<String, String> errors = new LinkedHashMap<>();
        checkRules(traitement, errors);
        throwIfAny(errors);
    }

    private static void checkRules(TraitementDTO traitement, Map<String, String> errors) {
        if (Objects.isNull(traitement.nom()) || traitement.nom().isBlank()) {
            errors.put("nom", MSG_REQUIRED);
        }
        if (Objects.isNull(traitement.client()) || Objects.isNull(traitement.client().id())) {
            errors.put("client", MSG_REQUIRED);
        }

        LocalDate dateIdentification = traitement.dateIdentification();
        LocalDate dateMiseAJour = traitement.dateMiseAJour();
        if (Objects.isNull(dateIdentification)) {
            errors.put("dateIdentification", MSG_REQUIRED);
        } else if (Objects.nonNull(dateMiseAJour) && dateMiseAJour.isBefore(dateIdentification)) {
            errors.put("dateMiseAJour", MSG_DATE_MISE_A_JOUR);
        }
    }

    private static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new InvalidTraitementException(errors);
        }
    }
}
