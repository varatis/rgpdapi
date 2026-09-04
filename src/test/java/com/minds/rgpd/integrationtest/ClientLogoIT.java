package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.ClientLogoInfoDTO;
import com.minds.rgpd.business.utilities.FormatImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql(scripts = "classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class ClientLogoIT extends AbstractITSpring {

    /** Client « La breteche » du script d'initialisation. */
    private static final UUID CLIENT_ID = UUID.fromString("0e4bf889-fea0-46ac-894d-ca39cbf00359");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void deposeUnLogoPuisLeRelit() throws Exception {
        byte[] png = pngMinimal();

        String reponse = deposerLogo(png, "acme-logo.png");

        ClientLogoInfoDTO info = mapper.readValue(reponse, ClientLogoInfoDTO.class);
        assertThat(info.nomFichier()).isEqualTo("acme-logo.png");
        assertThat(info.taille()).isEqualTo(png.length);
        assertThat(info.contentType()).isEqualTo(FormatImage.CONTENT_TYPE_PNG);
        assertThat(info.etag()).isNotBlank();

        MvcResult lecture = mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FormatImage.CONTENT_TYPE_PNG))
                .andReturn();

        assertThat(lecture.getResponse().getContentAsByteArray()).isEqualTo(png);
        assertThat(lecture.getResponse().getHeader(HttpHeaders.ETAG))
                .isEqualTo("\"%s\"".formatted(info.etag()));
        assertThat(lecture.getResponse().getHeader(HttpHeaders.CACHE_CONTROL))
                .contains("max-age=3600", "private");
    }

    @Test
    void repondSansContenuQuandLEtagEstInchange() throws Exception {
        deposerLogo(pngMinimal(), "acme-logo.png");

        String etag = mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID))
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void remplaceLeLogoExistant() throws Exception {
        deposerLogo(pngMinimal(), "ancien.png");
        byte[] jpeg = jpegMinimal();

        deposerLogo(jpeg, "nouveau.jpg");

        MvcResult lecture = mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, FormatImage.CONTENT_TYPE_JPEG))
                .andReturn();
        assertThat(lecture.getResponse().getContentAsByteArray()).isEqualTo(jpeg);
    }

    @Test
    void refuseUnSvgMemeSilEstDeclareCommeImage() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);

        envoyerLogo(svg, "logo.svg", "image/svg+xml")
                .andExpect(status().isBadRequest());
    }

    @Test
    void refuseUnFichierDeclarePngSansLaSignatureCorrespondante() throws Exception {
        byte[] faux = "ceci n est pas une image".getBytes(StandardCharsets.UTF_8);

        envoyerLogo(faux, "logo.png", MediaType.IMAGE_PNG_VALUE)
                .andExpect(status().isBadRequest());
    }

    @Test
    void renvoieNotFoundQuandLeClientNaPasDeLogo() throws Exception {
        mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/clients/{id}/logo/info", CLIENT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void supprimeLeLogo() throws Exception {
        deposerLogo(pngMinimal(), "acme-logo.png");

        mockMvc.perform(delete("/clients/{id}/logo", CLIENT_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/clients/{id}/logo", CLIENT_ID))
                .andExpect(status().isNotFound());
    }

    private String deposerLogo(byte[] contenu, String nomFichier) throws Exception {
        return envoyerLogo(contenu, nomFichier, MediaType.IMAGE_PNG_VALUE)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * MockMvc ne propose pas de constructeur multipart en PUT : on bascule la
     * méthode après coup, comme le ferait le navigateur.
     */
    private org.springframework.test.web.servlet.ResultActions envoyerLogo(
            byte[] contenu, String nomFichier, String contentTypeAnnonce) throws Exception {
        return mockMvc.perform(multipart("/clients/{id}/logo", CLIENT_ID)
                .file(new MockMultipartFile("file", nomFichier, contentTypeAnnonce, contenu))
                .with(requete -> {
                    requete.setMethod("PUT");
                    return requete;
                }));
    }

    /** Octets de signature PNG suivis d'un remplissage : seule la signature est contrôlée. */
    private byte[] pngMinimal() {
        return avecSignature(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
    }

    private byte[] jpegMinimal() {
        return avecSignature(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
    }

    private byte[] avecSignature(byte[] signature) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        sortie.writeBytes(signature);
        sortie.writeBytes("contenu-de-test".getBytes(StandardCharsets.UTF_8));
        return sortie.toByteArray();
    }
}
