package com.minds.rgpd.business.utilities;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FormatImageTest {

    @Test
    void reconnaitUnPng() {
        byte[] png = avecSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        assertThat(FormatImage.detectContentType(png))
                .contains(FormatImage.CONTENT_TYPE_PNG);
    }

    @Test
    void reconnaitUnJpeg() {
        byte[] jpeg = avecSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        assertThat(FormatImage.detectContentType(jpeg))
                .contains(FormatImage.CONTENT_TYPE_JPEG);
    }

    @Test
    void reconnaitUnWebp() {
        // Conteneur RIFF : 4 octets de signature, 4 octets de taille, puis "WEBP".
        ByteArrayOutputStream webp = new ByteArrayOutputStream();
        webp.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        webp.writeBytes(new byte[]{0x1A, 0x00, 0x00, 0x00});
        webp.writeBytes("WEBPVP8 ".getBytes(StandardCharsets.US_ASCII));

        assertThat(FormatImage.detectContentType(webp.toByteArray()))
                .contains(FormatImage.CONTENT_TYPE_WEBP);
    }

    @Test
    void refuseUnRiffQuiNestPasDuWebp() {
        // Un WAV est aussi un conteneur RIFF : seuls les octets 8 à 11 les distinguent.
        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        wav.writeBytes("RIFF".getBytes(StandardCharsets.US_ASCII));
        wav.writeBytes(new byte[]{0x1A, 0x00, 0x00, 0x00});
        wav.writeBytes("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));

        assertThat(FormatImage.detectContentType(wav.toByteArray())).isEmpty();
    }

    @Test
    void refuseUnSvg() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(FormatImage.detectContentType(svg)).isEmpty();
    }

    @Test
    void refuseUnGif() {
        byte[] gif = avecSignature("GIF89a".getBytes(StandardCharsets.US_ASCII));

        assertThat(FormatImage.detectContentType(gif)).isEmpty();
    }

    @Test
    void refuseUnContenuTropCourtPourPorterUneSignature() {
        assertThat(FormatImage.detectContentType(new byte[]{(byte) 0x89, 'P'})).isEmpty();
        assertThat(FormatImage.detectContentType(new byte[0])).isEmpty();
        assertThat(FormatImage.detectContentType(null)).isEqualTo(Optional.empty());
    }

    private byte[] avecSignature(byte[] signature) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        sortie.writeBytes(signature);
        sortie.writeBytes("contenu-de-test".getBytes(StandardCharsets.UTF_8));
        return sortie.toByteArray();
    }
}
