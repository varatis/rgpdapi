package com.minds.rgpd.business.utilities;

import com.minds.rgpd.persistence.entities.Definition;
import com.minds.rgpd.persistence.entities.Duree;
import com.minds.rgpd.persistence.entities.Etablissement;
import com.minds.rgpd.persistence.entities.ResponsableTraitement;
import com.minds.rgpd.persistence.entities.Traitement;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compare deux états d'un traitement pour produire le motif d'historisation (RG1).
 * <p>
 * Le motif est calculé plutôt que saisi : sans lui, l'historique se réduirait à
 * « modification », ce qui ne permet pas de reconstituer l'évolution du registre.
 * L'utilisateur peut compléter l'historique par une entrée saisie (CA4).
 */
public final class TraitementDiff {

    /** Champs techniques ou dérivés qui n'ont pas à apparaître dans l'historique. */
    private static final List<String> CHAMPS_IGNORES = List.of(
            "identifiant", "client", "historiqueTraitement", "version", "dateMiseAJour");

    private TraitementDiff() {
    }

    /**
     * Motif décrivant les écarts entre {@code avant} et {@code apres},
     * ou {@code null} si les deux états sont équivalents.
     */
    public static String motifDeModification(Map<String, String> avant, Map<String, String> apres) {
        List<String> ecarts = new ArrayList<>();
        for (Map.Entry<String, String> entree : apres.entrySet()) {
            String valeurAvant = avant.get(entree.getKey());
            String valeurApres = entree.getValue();
            if (!Objects.equals(valeurAvant, valeurApres)) {
                ecarts.add("%s : « %s » → « %s »".formatted(
                        entree.getKey(), abrege(valeurAvant), abrege(valeurApres)));
            }
        }
        if (ecarts.isEmpty()) {
            return null;
        }
        return String.join(" ; ", ecarts);
    }

    /**
     * Photographie des champs métier d'un traitement, sous forme lisible.
     * À prendre avant puis après la modification, sur l'entité managée.
     */
    public static Map<String, String> snapshot(Traitement traitement) {
        Map<String, String> valeurs = new LinkedHashMap<>();
        if (Objects.isNull(traitement)) {
            return valeurs;
        }
        for (Field champ : Traitement.class.getDeclaredFields()) {
            if (champ.isSynthetic() || CHAMPS_IGNORES.contains(champ.getName())) {
                continue;
            }
            champ.setAccessible(true);
            try {
                valeurs.put(champ.getName(), representer(champ.get(traitement)));
            } catch (IllegalAccessException e) {
                // Un champ inaccessible ne doit pas empêcher l'enregistrement de la modification.
                valeurs.put(champ.getName(), null);
            }
        }
        return valeurs;
    }

    private static String representer(Object valeur) {
        if (Objects.isNull(valeur)) {
            return null;
        }
        if (valeur instanceof Definition definition) {
            return definition.getValeur();
        }
        if (valeur instanceof Duree duree) {
            return duree.getValeur();
        }
        if (valeur instanceof ResponsableTraitement responsable) {
            return responsable.getValeur();
        }
        if (valeur instanceof LocalDate date) {
            return date.toString();
        }
        if (valeur instanceof List<?> liste) {
            // Les listes sont triées : l'ordre de chargement d'une collection JPA
            // n'est pas garanti et ne doit pas passer pour une modification.
            return liste.stream()
                    .map(TraitementDiff::representerElement)
                    .sorted()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
        return String.valueOf(valeur);
    }

    private static String representerElement(Object element) {
        if (element instanceof Etablissement etablissement) {
            return Objects.toString(etablissement.getNom(), "");
        }
        return String.valueOf(element);
    }

    /** L'historique reste lisible : les champs longs sont tronqués. */
    private static String abrege(String valeur) {
        if (Objects.isNull(valeur)) {
            return "";
        }
        String surUneLigne = valeur.replaceAll("\\R", " ").trim();
        return surUneLigne.length() <= 80 ? surUneLigne : surUneLigne.substring(0, 77) + "...";
    }
}
