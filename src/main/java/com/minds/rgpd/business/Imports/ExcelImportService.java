package com.minds.rgpd.business.Imports;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExcelImportService {

    public <T> ImportResult<T> importSheet(
            Workbook workbook,
            ImportSpecification<T> specification) {

        Sheet sheet = workbook.getSheet(
                specification.sheetName());

        if (sheet == null) {
            throw new IllegalArgumentException(
                    "Sheet not found: " +
                    specification.sheetName());
        }

        Map<String, List<Integer>> columnIndexes =
                readColumnIndexes(sheet, specification);

        List<T> imported = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();

        int firstDataRow =
                specification.headerRows();

        for (int rowIndex = firstDataRow;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row poiRow = sheet.getRow(rowIndex);

            if (poiRow == null || isEmptyRow(poiRow)) {
                continue;
            }

            ExcelRow row =
                    new ExcelRow(poiRow, columnIndexes);

            List<String> validationErrors =
                    validateRequiredColumns(
                            row,
                            specification.columns());

            if (!validationErrors.isEmpty()) {

                for (String error : validationErrors) {
                    errors.add(
                            new ImportError(
                                    rowIndex + 1,
                                    error));
                }

                continue;
            }

            try {
                T model =
                        specification.mapper().apply(row);

                imported.add(model);

            } catch (ExcelParsingException e) {

                // Erreur de données attendue : on la rattache à la ligne et on continue.
                // Toute autre exception (technique, inattendue) remonte et interrompt
                // l'import au lieu d'être masquée dans le rapport.
                errors.add(
                        new ImportError(
                                rowIndex + 1,
                                e.getMessage()));
            }
        }

        return new ImportResult<>(
                imported,
                errors);
    }

    private Map<String, List<Integer>> readColumnIndexes(
            Sheet sheet,
            ImportSpecification<?> specification) {

        Row headerRow =
                sheet.getRow(specification.headerRows() - 1);

        if (headerRow == null) {
            throw new IllegalArgumentException(
                    "Header row not found");
        }

        Map<String, List<Integer>> result =
                new HashMap<>();

        for (Cell cell : headerRow) {

            String columnName =
                    cell.getStringCellValue().trim();
            result
                .computeIfAbsent(columnName, i -> new ArrayList<>())
                .add(cell.getColumnIndex());       
        }

        for (String requiredColumnName :
                specification.columns()) {

            if (!result.containsKey(requiredColumnName)) {

                throw new IllegalArgumentException(
                        "Required column does not exist: "
                        + requiredColumnName);
            }
        }

        return result;
    }

    private List<String> validateRequiredColumns(
            ExcelRow row,
            List<String> requiredColumnNames) {

        List<String> errors = new ArrayList<>();

        for (String requiredColumnName : requiredColumnNames) {

            if (row.isEmpty(requiredColumnName)) {

                errors.add(
                        "Column '" +
                        requiredColumnName +
                        "' is required");
            }
        }

        return errors;
    }

    private boolean isEmptyRow(Row row) {

        // Une ligne "vide" au sens Excel peut conserver des cellules de type STRING
        // contenant "" ou des espaces (contenu effacé, mise en forme résiduelle) : elles
        // ne sont pas de type BLANK. On considère donc la ligne vide si toutes ses
        // cellules se réduisent à du vide, comme le fait ExcelRow.getString.
        for (Cell cell : row) {

            if (cell == null
                    || cell.getCellType() == CellType.BLANK) {
                continue;
            }

            String value = cell.toString();

            if (value != null && !value.isBlank()) {
                return false;
            }
        }

        return true;
    }
}
