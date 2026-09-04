package com.minds.rgpd.business.utilities;

import java.util.Arrays;
import java.util.Optional;

/**
 * Détection du format d'une image à partir de ses octets de signature.
 * <p>
 * Le {@code Content-Type} annoncé par le navigateur n'est pas vérifié : c'est
 * une simple déclaration du client. On détermine ici le type réellement reçu et
 * c'est celui-là qui est stocké puis renvoyé à la lecture.
 * <p>
 * SVG est volontairement absent de la liste : un SVG servi depuis l'origine de
 * l'API exécute le contenu de ses balises {@code <script>}, ce qui en ferait un
 * vecteur de XSS stocké sur la console d'administration.
 */
public final class FormatImage {

    private static final byte[] SIGNATURE_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] SIGNATURE_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] SIGNATURE_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] SIGNATURE_WEBP = {'W', 'E', 'B', 'P'};

    public static final String CONTENT_TYPE_PNG = "image/png";
    public static final String CONTENT_TYPE_JPEG = "image/jpeg";
    public static final String CONTENT_TYPE_WEBP = "image/webp";

    private FormatImage() {
    }

    /**
     * @return le type MIME correspondant aux octets de signature, ou
     * {@link Optional#empty()} si le format n'est pas accepté.
     */
    public static Optional<String> detectContentType(byte[] contenu) {
        if (startBy(contenu, SIGNATURE_PNG, 0)) {
            return Optional.of(CONTENT_TYPE_PNG);
        }
        if (startBy(contenu, SIGNATURE_JPEG, 0)) {
            return Optional.of(CONTENT_TYPE_JPEG);
        }
        // WebP : conteneur RIFF, le format réel est porté par les octets 8 à 11.
        if (startBy(contenu, SIGNATURE_RIFF, 0) && startBy(contenu, SIGNATURE_WEBP, 8)) {
            return Optional.of(CONTENT_TYPE_WEBP);
        }
        return Optional.empty();
    }

    private static boolean startBy(byte[] contenu, byte[] signature, int position) {
        if (contenu == null || contenu.length < position + signature.length) {
            return false;
        }
        return Arrays.equals(contenu, position, position + signature.length, signature, 0, signature.length);
    }
}
