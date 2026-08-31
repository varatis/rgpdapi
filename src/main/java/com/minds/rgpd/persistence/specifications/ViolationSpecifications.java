package com.minds.rgpd.persistence.specifications;

import com.minds.rgpd.business.dtos.ViolationFilterCriteria;
import com.minds.rgpd.business.enums.ViolationStatut;
import com.minds.rgpd.persistence.entities.Violation;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public class ViolationSpecifications {

    static final char LIKE_ESCAPE_CHAR = '\\';

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_NOM = "nom";
    private static final String FIELD_NATURE_VIOLATION = "natureViolation";
    private static final String FIELD_DONNEES_CONCERNEES = "donneesConcernees";
    private static final String FIELD_RISQUE_ELEVE = "risqueEleveDroitsLibertes";
    private static final String FIELD_STATUT = "statut";
    private static final String FIELD_DATE_VIOLATION = "dateViolation";
    private static final String FIELD_NOMBRE_PERSONNES = "nombrePersonnesConcernees";

    private ViolationSpecifications() {
    }

    public static Specification<Violation> search(String clientNom, ViolationFilterCriteria criteria) {
        Assert.hasText(clientNom, "clientNom ne peut pas être vide");
        ViolationFilterCriteria safe = criteria != null ? criteria : ViolationFilterCriteria.empty();
        return Specification.allOf(
                hasClientNom(clientNom),
                containsNatureViolation(safe.natureViolation()),
                containsDonneesConcernees(safe.donneesConcernees()),
                hasRisqueEleve(safe.risqueEleveDroitsLibertes()),
                hasStatut(safe.statut()),
                dateViolationDepuis(safe.dateViolationDebut()),
                dateViolationJusqua(safe.dateViolationFin()),
                nombrePersonnesAuMoins(safe.nombrePersonnesConcerneesMin()),
                nombrePersonnesAuPlus(safe.nombrePersonnesConcerneesMax())
        );
    }

    private static Specification<Violation> hasClientNom(String clientNom) {
        if (!StringUtils.hasText(clientNom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_CLIENT).get(FIELD_NOM), clientNom);
    }

    private static Specification<Violation> containsNatureViolation(String natureViolation) {
        if (!StringUtils.hasText(natureViolation)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_NATURE_VIOLATION)), likePattern(natureViolation), LIKE_ESCAPE_CHAR);
    }

    private static Specification<Violation> containsDonneesConcernees(String donneesConcernees) {
        if (!StringUtils.hasText(donneesConcernees)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_DONNEES_CONCERNEES)), likePattern(donneesConcernees), LIKE_ESCAPE_CHAR);
    }

    private static Specification<Violation> hasRisqueEleve(Boolean risqueEleveDroitsLibertes) {
        if (risqueEleveDroitsLibertes == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_RISQUE_ELEVE), risqueEleveDroitsLibertes);
    }

    private static Specification<Violation> hasStatut(ViolationStatut statut) {
        if (statut == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_STATUT), statut);
    }

    /** Borne incluse : les violations survenues à cette date sont retenues. */
    private static Specification<Violation> dateViolationDepuis(LocalDate debut) {
        if (debut == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.<LocalDate>get(FIELD_DATE_VIOLATION), debut);
    }

    /** Borne incluse : les violations survenues à cette date sont retenues. */
    private static Specification<Violation> dateViolationJusqua(LocalDate fin) {
        if (fin == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.<LocalDate>get(FIELD_DATE_VIOLATION), fin);
    }

    private static Specification<Violation> nombrePersonnesAuMoins(Integer min) {
        if (min == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.<Integer>get(FIELD_NOMBRE_PERSONNES), min);
    }

    private static Specification<Violation> nombrePersonnesAuPlus(Integer max) {
        if (max == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.<Integer>get(FIELD_NOMBRE_PERSONNES), max);
    }

    static String likePattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped.toLowerCase() + "%";
    }
}
