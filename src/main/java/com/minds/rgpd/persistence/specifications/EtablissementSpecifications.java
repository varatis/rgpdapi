package com.minds.rgpd.persistence.specifications;

import com.minds.rgpd.business.dtos.EtablissementFilterCriteria;
import com.minds.rgpd.persistence.entities.Etablissement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class EtablissementSpecifications {

    static final char LIKE_ESCAPE_CHAR = '\\';

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_NOM = "nom";
    private static final String FIELD_DEPARTEMENT = "departement";
    private static final String FIELD_PRINCIPAL = "principal";

    private EtablissementSpecifications() {
    }

    public static Specification<Etablissement> search(String clientNom, EtablissementFilterCriteria criteria) {
        Assert.hasText(clientNom, "clientNom ne peut pas être vide");
        EtablissementFilterCriteria safe = criteria != null ? criteria : EtablissementFilterCriteria.empty();
        return Specification.allOf(
                hasClientNom(clientNom),
                containsNom(safe.nom()),
                hasDepartement(safe.departement()),
                hasPrincipal(safe.principal())
        );
    }

    private static Specification<Etablissement> hasClientNom(String clientNom) {
        if (!StringUtils.hasText(clientNom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_CLIENT).get(FIELD_NOM), clientNom);
    }

    private static Specification<Etablissement> containsNom(String nom) {
        if (!StringUtils.hasText(nom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_NOM)), likePattern(nom), LIKE_ESCAPE_CHAR);
    }

    /**
     * Egalite stricte : un code de departement est saisi en entier, et "2A" ne
     * doit pas remonter sur une recherche de "2".
     */
    private static Specification<Etablissement> hasDepartement(String departement) {
        if (!StringUtils.hasText(departement)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(FIELD_DEPARTEMENT)), departement.trim().toLowerCase());
    }

    private static Specification<Etablissement> hasPrincipal(Boolean principal) {
        if (principal == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get(FIELD_PRINCIPAL), principal);
    }

    static String likePattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped.toLowerCase() + "%";
    }
}
