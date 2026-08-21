package com.minds.rgpd.business.Imports;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExcelImportServiceTest {

    private final ExcelImportService service = new ExcelImportService();

    @Test
    void ignoreLesLignesSansAucunChampMetierEtImporteLesLignesValides() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Registre de traitement");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nom du traitement");
            header.createCell(1).setCellValue("Gestionnaire de la mise en œuvre du traitement");
            header.createCell(2).setCellValue("Finalité principale");

            Row valide = sheet.createRow(1);
            valide.createCell(0).setCellValue("Paie");
            valide.createCell(1).setCellValue("Dupont");
            valide.createCell(2).setCellValue("RH");

            Row piedFormules = sheet.createRow(2);
            piedFormules.createCell(3).setCellValue("84");

            Row incomplet = sheet.createRow(3);
            incomplet.createCell(0).setCellValue("Commercial");
            incomplet.createCell(2).setCellValue("Ventes");

            @SuppressWarnings("unchecked")
            ImportSpecification<String> spec = new ImportSpecification<>(
                    "Registre de traitement",
                    false,
                    1,
                    List.of(
                            "Nom du traitement",
                            "Gestionnaire de la mise en œuvre du traitement",
                            "Finalité principale"
                    ),
                    row -> row.getString("Nom du traitement"),
                    item -> false,
                    mock(JpaRepository.class)
            );

            ImportResult<String> result = service.importSheet(workbook, spec);

            assertThat(result.imported()).containsExactly("Paie");
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().rowNumber()).isEqualTo(4);
            assertThat(result.errors().getFirst().message()).contains("Gestionnaire");
        }
    }
}
