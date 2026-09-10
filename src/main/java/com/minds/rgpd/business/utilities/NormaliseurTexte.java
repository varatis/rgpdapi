package com.minds.rgpd.business.utilities;

import java.text.Normalizer;
import java.util.Locale;

public final class NormaliseurTexte {

    private NormaliseurTexte() {
    }

    public static String normaliser(String valeur) {
        if (valeur == null) {
            return null;
        }
        String sansAccents = Normalizer.normalize(valeur.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sansAccents.toLowerCase(Locale.ROOT);
    }

    public static boolean contient(String valeur, String filtre) {
        String filtreNormalise = normaliser(filtre);
        if (filtreNormalise == null || filtreNormalise.isEmpty()) {
            return true;
        }
        String valeurNormalisee = normaliser(valeur);
        return valeurNormalisee != null && valeurNormalisee.contains(filtreNormalise);
    }
}
