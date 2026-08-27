package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.Imports.ExcelImportService;
import com.minds.rgpd.business.Imports.ImportResult;
import com.minds.rgpd.business.Imports.ImportSpecification;
import com.minds.rgpd.business.Imports.ImportSpecifications;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.InfoFichierDTO;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.persistence.entities.Client;
import com.minds.rgpd.persistence.entities.Traitement;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FichierServiceImplTest {

    private static final UUID CLIENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String NOM_FICHIER = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private ExcelImportService importer;

    @Mock
    private ImportSpecifications importSpecifications;

    @Mock
    private TraitementRepository traitementRepository;

    @Mock
    private PreconisationRepository preconisationRepository;

    @InjectMocks
    private FichierServiceImpl fichierService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(CLIENT_UUID);
        client.setNom("La breteche");
    }

    @Test
    void importFichier_remplaceLEtatPrecedentDuRegistreDuClient() {
        // GIVEN
        Traitement fichierTraitement1 = Traitement.builder().idFonctionnel(1).nom("Pré inscription").client(client).build();
        Traitement fichierTraitement2 = Traitement.builder().idFonctionnel(2).nom("Dossier social").client(client).build();

        when(clientRepository.findByNom("La breteche")).thenReturn(Optional.of(client));
        when(clientMapper.map(client)).thenReturn(ClientDTO.builder().id(CLIENT_UUID).nom("La breteche").build());
        ImportSpecification<Traitement> specification = new ImportSpecification<>(
                "Registre de traitement", false, 6, List.of("ID"), row -> null);
        when(importSpecifications.traitement(client, "3")).thenReturn(specification);
        doReturn(new ImportResult<>(List.of(fichierTraitement1, fichierTraitement2), List.of()))
                .when(importer).importSheet(any(XSSFWorkbook.class), any(ImportSpecification.class));

        // WHEN
        InfoFichierDTO resultat = fichierService.importFichier(fichier(), InfoFichierDTO.builder()
                .nomFichier(NOM_FICHIER)
                .statusFichier("KO")
        ).build();

        // THEN
        // RG2 : l'état précédent est remplacé — d'abord les préconisations, puis les
        // liens établissement, puis les traitements, enfin l'insertion du fichier.
        InOrder ordre = inOrder(preconisationRepository, traitementRepository);
        ordre.verify(preconisationRepository).deleteByClient(client);
        ordre.verify(traitementRepository).deleteLiensEtablissementsByClient(CLIENT_UUID);
        ordre.verify(traitementRepository).deleteByClient(client);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Traitement>> captor = ArgumentCaptor.forClass(List.class);
        ordre.verify(traitementRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(fichierTraitement1, fichierTraitement2);

        assertThat(resultat.statusFichier()).isEqualTo("OK");
        assertThat(resultat.dateFinTraitement()).isNotNull();
    }

    @Test
    void importFichier_aucuneLigneTraitable_neRemplaceRien() {
        // GIVEN
        when(clientRepository.findByNom("La breteche")).thenReturn(Optional.of(client));
        when(clientMapper.map(client)).thenReturn(ClientDTO.builder().id(CLIENT_UUID).nom("La breteche").build());
        ImportSpecification<Traitement> specification = new ImportSpecification<>(
                "Registre de traitement", false, 6, List.of("ID"), row -> null);
        when(importSpecifications.traitement(client, "3")).thenReturn(specification);
        doReturn(new ImportResult<Traitement>(List.of(), List.of()))
                .when(importer).importSheet(any(XSSFWorkbook.class), any(ImportSpecification.class));

        // WHEN
        InfoFichierDTO resultat = fichierService.importFichier(fichier(), InfoFichierDTO.builder()
                .nomFichier(NOM_FICHIER)
                .statusFichier("KO")
        ).build();

        // THEN
        // RG3 : sans contenu exploitable, aucune donnée du client n'est touchée.
        assertThat(resultat.statusFichier()).contains("aucune ligne traitable").isNotEqualTo("OK");
        verify(preconisationRepository, never()).deleteByClient(any());
        verify(traitementRepository, never()).deleteByClient(any());
        verify(traitementRepository, never()).deleteLiensEtablissementsByClient(any());
        verify(traitementRepository, never()).saveAll(any());
    }

    @Test
    void importFichier_clientInconnu_neRemplaceRien() {
        // GIVEN
        when(clientRepository.findByNom("La breteche")).thenReturn(Optional.empty());

        // WHEN
        InfoFichierDTO resultat = fichierService.importFichier(fichier(), InfoFichierDTO.builder()
                .nomFichier(NOM_FICHIER)
                .statusFichier("KO")
        ).build();

        // THEN
        assertThat(resultat.statusFichier()).contains("Client absent de la base de données");
        verifyNoImportRepositoryWrites();
    }

    @Test
    void importFichier_nomNonConforme_rejeteSansTraitement() {
        // GIVEN
        MockMultipartFile fichier = new MockMultipartFile(
                "file",
                "registre-non-conforme.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookVide()
        );

        // WHEN
        InfoFichierDTO resultat = fichierService.importFichier(fichier, InfoFichierDTO.builder()
                .nomFichier("registre-non-conforme.xlsx")
                .statusFichier("KO")
        ).build();

        // THEN
        assertThat(resultat.statusFichier()).contains("n'a pas le nom formaté comme attendu");
        verifyNoImportRepositoryWrites();
    }

    private void verifyNoImportRepositoryWrites() {
        verify(preconisationRepository, never()).deleteByClient(any());
        verify(traitementRepository, never()).deleteByClient(any());
        verify(traitementRepository, never()).deleteLiensEtablissementsByClient(any());
        verify(traitementRepository, never()).saveAll(any());
    }

    private MockMultipartFile fichier() {
        return new MockMultipartFile(
                "file",
                NOM_FICHIER,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbookVide()
        );
    }

    /** Un classeur vide suffit : la lecture des feuilles est simulée par le mock de ExcelImportService. */
    private byte[] workbookVide() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            workbook.write(sortie);
            return sortie.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Classeur de test impossible à créer", e);
        }
    }
}
