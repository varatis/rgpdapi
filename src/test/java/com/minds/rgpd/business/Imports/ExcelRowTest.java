package com.minds.rgpd.business.Imports;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelRowTest {

    @Test
    void getOptionalString_prendLaPremiereColonneRenseignee() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("En cours");

            ExcelRow excelRow = new ExcelRow(row, Map.of(
                    "État d'avancement", List.of(0),
                    "Statut", List.of(1)
            ));

            assertThat(excelRow.hasColumn("État d'avancement")).isTrue();
            assertThat(excelRow.hasColumn("Inconnu")).isFalse();
            assertThat(excelRow.getOptionalString("Avancement", "État d'avancement", "Statut"))
                    .isEqualTo("En cours");
            assertThat(excelRow.getOptionalString("Inconnu", "Absent")).isNull();
        }
    }
}
