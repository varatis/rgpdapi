package com.minds.rgpd.persistence.specifications;

import com.minds.rgpd.persistence.entities.Preconisation;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.UUID;

public class PreconisationSpecifications {

    static final char LIKE_ESCAPE_CHAR = '\\';

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_NOM = "nom";
    private static final String FIELD_LIBELLE = "libelle";
    private static final String FIELD_ETAT_AVANCEMENT = "etatAvancement";
    private static final String FIELD_TRAITEMENT = "traitement";
    private static final String FIELD_IDENTIFIANT = "identifiant";

    public static Specification<Preconisation> search(
            String clientNom,
            String libelle,
            String etatAvancement,
            UUID idTraitement
    ) {
        Assert.hasText(clientNom, "clientNom ne peut pas être vide");
        return Specification.allOf(
                hasClientNom(clientNom),
                containsLibelle(libelle),
                hasEtatAvancement(etatAvancement),
                hasTraitement(idTraitement)
        );
    }

    private static Specification<Preconisation> hasClientNom(String clientNom) {
        if (!StringUtils.hasText(clientNom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_CLIENT).get(FIELD_NOM), clientNom);
    }

    private static Specification<Preconisation> containsLibelle(String libelle) {
        if (!StringUtils.hasText(libelle)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_LIBELLE)), likePattern(libelle), LIKE_ESCAPE_CHAR);
    }

    private static Specification<Preconisation> hasEtatAvancement(String etatAvancement) {
        if (!StringUtils.hasText(etatAvancement)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(FIELD_ETAT_AVANCEMENT)), etatAvancement.toLowerCase());
    }

    private static Specification<Preconisation> hasTraitement(UUID idTraitement) {
        if (idTraitement == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_TRAITEMENT).get(FIELD_IDENTIFIANT), idTraitement);
    }

    static String likePattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped.toLowerCase() + "%";
    }
}
