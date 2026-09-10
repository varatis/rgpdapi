package com.minds.rgpd.business.services.impl;

import com.minds.rgpd.business.Imports.ExcelImportService;
import com.minds.rgpd.business.Imports.ImportSpecifications;
import com.minds.rgpd.business.dtos.ClientDTO;
import com.minds.rgpd.business.dtos.TraitementDTO;
import com.minds.rgpd.business.services.HistorisationService;
import com.minds.rgpd.business.utilities.mappers.ClientMapper;
import com.minds.rgpd.business.utilities.mappers.TraitementMapper;
import com.minds.rgpd.persistence.repositories.ClientRepository;
import com.minds.rgpd.persistence.repositories.PreconisationRepository;
import com.minds.rgpd.persistence.repositories.TraitementRepository;
import com.minds.rgpd.persistence.repositories.ViolationRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FichierExportTemplateTest {

    private static final String FILENAME = "La breteche_CREATIVE_Registre RGPD_ed3.25.xlsx";
    private static final String CLIENT = "La breteche";

    private FichierServiceImpl serviceAvec(TraitementRepository traitementRepository,
                                           TraitementMapper traitementMapper) {
        return new FichierServiceImpl(
                mock(ClientRepository.class),
                mock(ClientMapper.class),
                mock(ExcelImportService.class),
                mock(ImportSpecifications.class),
                traitementRepository,
                mock(PreconisationRepository.class),
                mock(ViolationRepository.class),
                traitementMapper,
                mock(HistorisationService.class)
        );
    }

    private ClientDTO client() {
        return ClientDTO.builder()
                .id(UUID.randomUUID())
                .nom(CLIENT)
                .statut("ACTIF")
                .version("3.25")
                .dateVersion(LocalDate.now())
                .durees(List.of())
                .definitions(List.of())
                .responsablesTraitement(List.of())
                .build();
    }

    private List<TraitementDTO> troisTraitements() {
        TraitementDTO t1 = TraitementDTO.builder()
                .identifiant(UUID.randomUUID()).idFonctionnel(1).nom("Traitement alpha")
                .dateIdentification(LocalDate.of(2024, 1, 15))
                .donneesConcernees("Données diverses").build();
        TraitementDTO t2 = TraitementDTO.builder()
                .identifiant(UUID.randomUUID()).idFonctionnel(2).nom("Traitement beta")
                .dateIdentification(LocalDate.of(2024, 2, 20))
                .donneesConcernees("Autres données").build();
        TraitementDTO t3 = TraitementDTO.builder()
                .identifiant(UUID.randomUUID()).idFonctionnel(3).nom("Traitement gamma")
                .dateIdentification(LocalDate.of(2024, 3, 25))
                .donneesConcernees("Encore des données").build();
        return List.of(t1, t2, t3);
    }

    @Test
    void exportDepuisTemplatePreserveDesignEtStructure() throws Exception {
        InputStream template = new ClassPathResource("rgpdFile/" + FILENAME).getInputStream();
        List<TraitementDTO> traitements = troisTraitements();

        TraitementRepository traitementRepository = mock(TraitementRepository.class);
        when(traitementRepository.findAll(any(Specification.class))).thenReturn(List.of());
        TraitementMapper traitementMapper = mock(TraitementMapper.class);
        when(traitementMapper.mapToDTOList(any())).thenReturn(traitements);

        FichierServiceImpl service = serviceAvec(traitementRepository, traitementMapper);

        byte[] bytes = service.generationExcelRegistreTraitements(
                client(), "La breteche_CREATIVE_Registre RGPD_ed3.25", template);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(7);
            assertThat(wb.getSheet("FR_Définitions")).isNotNull();
            assertThat(wb.getSheet("Sous-traitant DCP")).isNotNull();
            assertThat(wb.getSheet("Registre de traitement")).isNotNull();

            XSSFWorkbook xssf = (XSSFWorkbook) wb;
            assertThat(xssf.getStylesSource().getNumCellStyles()).isGreaterThan(1);
            assertThat(xssf.getTheme()).isNotNull();

            XSSFSheet registre = (XSSFSheet) wb.getSheet("Registre de traitement");

            assertThat(registre.getRow(5).getCell(1).getStringCellValue()).isEqualTo("ID");
            assertThat(registre.getRow(5).getCell(4).getStringCellValue())
                    .isEqualTo("Nom du traitement");

            assertThat(registre.getRow(0).getCell(1).getStringCellValue())
                    .contains("Grille de collecte / Registre des activités de traitement");

            assertThat(registre.getRow(6).getCell(1).getStringCellValue()).isEqualTo("1");
            assertThat(registre.getRow(6).getCell(4).getStringCellValue()).isEqualTo("Traitement alpha");
            assertThat(registre.getRow(8).getCell(4).getStringCellValue()).isEqualTo("Traitement gamma");

            String filterRef = registre.getCTWorksheet().getAutoFilter().getRef();
            assertThat(filterRef).isEqualTo("A6:HK9");
        }
    }

    @Test
    void exportSansTemplateGenereUnClasseurValide() throws Exception {
        List<TraitementDTO> traitements = List.of(
                TraitementDTO.builder()
                        .identifiant(UUID.randomUUID()).idFonctionnel(1).nom("Traitement alpha")
                        .dateIdentification(LocalDate.of(2024, 1, 15))
                        .donneesConcernees("Données diverses").build());

        TraitementRepository traitementRepository = mock(TraitementRepository.class);
        when(traitementRepository.findAll(any(Specification.class))).thenReturn(List.of());
        TraitementMapper traitementMapper = mock(TraitementMapper.class);
        when(traitementMapper.mapToDTOList(any())).thenReturn(traitements);

        FichierServiceImpl service = serviceAvec(traitementRepository, traitementMapper);

        byte[] bytes = service.generationExcelRegistreTraitements(
                client(), "ClientX_CREATIVE_Registre RGPD_ed1.0", null);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet registre = wb.getSheet("Registre de traitement");
            assertThat(registre).isNotNull();
            assertThat(registre.getRow(5).getCell(1).getStringCellValue()).isEqualTo("ID");
            assertThat(registre.getRow(6).getCell(1).getStringCellValue()).isEqualTo("1");
            assertThat(registre.getRow(6).getCell(4).getStringCellValue()).isEqualTo("Traitement alpha");
        }
    }
}
