package com.minds.rgpd.persistence.specifications;

import com.minds.rgpd.persistence.entities.Traitement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class TraitementSpecifications {

    /**
     * Caractère d'échappement pour les patterns LIKE (doit correspondre à l'argument
     * {@code escapeChar} passé à {@link jakarta.persistence.criteria.CriteriaBuilder#like}).
     */
    static final char LIKE_ESCAPE_CHAR = '\\';

    private static final String FIELD_CLIENT = "client";
    private static final String FIELD_NOM = "nom";
    private static final String FIELD_GESTIONNAIRE_MISE_EN_OEUVRE = "gestionnaireMiseEnOeuvre";
    private static final String FIELD_FINALITE_PRINCIPALE = "finalitePrincipale";
    private static final String FIELD_VALEUR = "valeur";

    /**
     * Combine tous les critères optionnels en une seule Specification.
     * Les critères non renseignés (null/blank) sont automatiquement ignorés
     * via {@link Specification#unrestricted()} — pas de contrat implicite null → "".
     */
    public static Specification<Traitement> search(String clientNom, String nom, String gestionnaire, String finalite) {
        Assert.hasText(clientNom, "clientNom ne peut pas être vide");
        return Specification.allOf(
                hasClientNom(clientNom),
                containsNom(nom),
                containsGestionnaire(gestionnaire),
                containsFinalite(finalite)
        );
    }

    private static Specification<Traitement> hasClientNom(String clientNom) {
        if (!StringUtils.hasText(clientNom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.equal(root.get(FIELD_CLIENT).get(FIELD_NOM), clientNom);
    }

    /**
     * Recherche « contient » (LIKE %valeur%).
     * <p>
     * <b>Limitation perf :</b> le wildcard en préfixe empêche l'utilisation d'un index B-tree
     * classique sur la colonne. Acceptable tant que le volume de traitements reste modéré
     * et filtré par client. Si le volume croît, envisager un préfixe (LIKE 'valeur%'),
     * un index trigram (pg_trgm) ou une recherche full-text (tsvector).
     */
    private static Specification<Traitement> containsNom(String nom) {
        if (!StringUtils.hasText(nom)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_NOM)), likePattern(nom), LIKE_ESCAPE_CHAR);
    }

    /** @see #containsNom(String) pour la limitation d'index liée au wildcard préfixe */
    private static Specification<Traitement> containsGestionnaire(String gestionnaire) {
        if (!StringUtils.hasText(gestionnaire)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_GESTIONNAIRE_MISE_EN_OEUVRE)), likePattern(gestionnaire), LIKE_ESCAPE_CHAR);
    }

    /** @see #containsNom(String) pour la limitation d'index liée au wildcard préfixe */
    private static Specification<Traitement> containsFinalite(String finalite) {
        if (!StringUtils.hasText(finalite)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get(FIELD_FINALITE_PRINCIPALE).get(FIELD_VALEUR)), likePattern(finalite), LIKE_ESCAPE_CHAR);
    }

    /**
     * Construit un pattern LIKE « contient » en échappant les caractères spéciaux (%, _, \)
     * pour qu'une saisie utilisateur ne soit pas interprétée comme des wildcards SQL.
     * Package-private pour les tests unitaires.
     */
    static String likePattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped.toLowerCase() + "%";
    }
}
