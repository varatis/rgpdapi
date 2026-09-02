package com.minds.rgpd.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minds.rgpd.AbstractITSpring;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.HistorisationRegistreRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@Sql(scripts = "classpath:scripts/initialisation_import_fichier_TI.sql")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class EnvoiFichierIT extends AbstractITSpring {

    private static final String FILENAME = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";
    private static final String CLIENT = "La breteche";

    ObjectMapper mapper = new ObjectMapper();
    @Autowired
    TraitementRepository traitementRepository;
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    HistorisationRegistreRepository historisationRegistreRepository;
    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mapper.registerModule(new JavaTimeModule());
    }

    private MockMultipartFile registre() throws Exception {
        File realFile = new ClassPathResource("rgpdFile/" + FILENAME).getFile();
        return new MockMultipartFile(
                "file",
                realFile.getName(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(realFile.toPath())
        );
    }

    private Client client() {
        return clientRepository.findByNom(CLIENT).orElseThrow();
    }

    @Test
    void importFichier() throws Exception {
        // GIVEN
        MockMultipartFile file = registre();

        // WHEN : RG3, l'utilisateur confirme le remplacement de ses données
        String infoFichierDTOString = mockMvc.perform(multipart("/importFichierRgpd")
                        .file(file)
                        .param("confirmerRemplacement", "true"))
                .andReturn().getResponse().getContentAsString();

        // THEN
        assertThat(infoFichierDTOString).isNotNull();
        InfoFichierDTO infoFichierDTO = mapper.readValue(infoFichierDTOString, InfoFichierDTO.class);
        assertThat(infoFichierDTO.nomFichier()).isEqualTo(FILENAME);
        assertThat(infoFichierDTO.statusFichier()).isEqualTo("OK");
        // Le registre du client ne contient plus que les lignes du fichier (RG2) :
        // les 2 traitements initialisés par le script ont été remplacés.
        List<Traitement> traitementList = traitementRepository.findByClient(client());
        assertThat(traitementList).isNotNull().hasSize(80);
        assertThat(infoFichierDTO.nombreTraitementsRemplaces()).isEqualTo(2);
    }

    /** RG2 : l'import remplace l'état précédent des traitements du client. */
    @Test
    void importRemplaceLesTraitementsExistantsDuClient() throws Exception {
        Client client = client();
        assertThat(traitementRepository.findByClient(client)).isNotEmpty();

        String reponse = mockMvc.perform(multipart("/importFichierRgpd")
                        .file(registre())
                        .param("confirmerRemplacement", "true"))
                .andReturn().getResponse().getContentAsString();

        InfoFichierDTO info = mapper.readValue(reponse, InfoFichierDTO.class);
        assertThat(info.statusFichier()).isEqualTo("OK");
        assertThat(info.nombreTraitementsRemplaces()).isEqualTo(2);

        // Les traitements pré-existants du client ont disparu…
        assertThat(traitementRepository.findByClient(client))
                .extracting(Traitement::getNom)
                .doesNotContain("Gestion des salariés", "Suivi des ventes");
        // … et ceux des autres clients sont intacts.
        assertThat(traitementRepository.findByNomAndClient("Conformité RGPD",
                clientRepository.findByNom("Entreprise Alpha").orElseThrow())).isNotEmpty();
    }

    /** RG3 : sans confirmation, rien n'est modifié et l'aperçu est renvoyé. */
    @Test
    void importSansConfirmationNeModifieRien() throws Exception {
        Client client = client();
        List<Traitement> avant = traitementRepository.findByClient(client);

        MvcResult result = mockMvc.perform(multipart("/importFichierRgpd").file(registre()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        InfoFichierDTO info = mapper.readValue(result.getResponse().getContentAsString(), InfoFichierDTO.class);
        assertThat(info.confirmationRequise()).isTrue();
        assertThat(info.apercu()).isNotNull();
        assertThat(info.apercu().remplacementDonnees()).isTrue();
        assertThat(info.apercu().nombreTraitementsExistants()).isEqualTo(avant.size());
        assertThat(info.apercu().versionFichier()).isEqualTo("3.25");
        assertThat(info.apercu().urlExportPrealable()).isNotBlank();

        assertThat(traitementRepository.findByClient(client)).hasSameSizeAs(avant);
    }

    /** RG3 : l'aperçu est consultable avant même l'envoi du fichier. */
    @Test
    void apercuImportDecritLesConsequences() throws Exception {
        String reponse = mockMvc.perform(get("/importFichierRgpd/apercu").param("nomFichier", FILENAME))
                .andReturn().getResponse().getContentAsString();

        assertThat(reponse).contains("\"fichierValide\":true");
        assertThat(reponse).contains("\"remplacementDonnees\":true");
    }

    /** RG4 : la version du registre est reprise du fichier importé. */
    @Test
    void importMetAJourLaVersionDuRegistre() throws Exception {
        mockMvc.perform(multipart("/importFichierRgpd")
                .file(registre())
                .param("confirmerRemplacement", "true"));

        Client client = client();
        assertThat(client.getVersion()).isEqualTo("3.25");
        assertThat(client.getDateVersion()).isNotNull();
        // RG1 : l'import laisse une trace dans l'historique du registre.
        assertThat(historisationRegistreRepository.findByClientOrderByDateDesc(client))
                .isNotEmpty()
                .anySatisfy(h -> assertThat(h.getMotif()).contains("Import du registre"));
    }

    /** RG5 : les colonnes complémentaires du registre sont importées. */
    @Test
    void importChargeLesColonnesComplementaires() throws Exception {
        mockMvc.perform(multipart("/importFichierRgpd")
                .file(registre())
                .param("confirmerRemplacement", "true"));

        List<Traitement> traitements = traitementRepository.findByClient(client());
        assertThat(traitements)
                .anySatisfy(t -> assertThat(t.getImpactTraitement()).isNotNull())
                .anySatisfy(t -> assertThat(t.getScoreGlobal()).isNotNull())
                .anySatisfy(t -> assertThat(t.getCritereCollecteDonneesSensibles()).isTrue())
                .anySatisfy(t -> assertThat(t.getCriterePersonnesVulnerables()).isTrue())
                .anySatisfy(t -> assertThat(t.getCommentairesAnalyse()).isNotBlank());
    }
}
