package com.minds.rgpd.business.Imports;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExcelRow {

    private final Row row;
    private final Map<String, List<Integer>> columnIndexes;

    public ExcelRow(Row row, Map<String, List<Integer>> columnIndexes) {
        this.row = row;
        this.columnIndexes = columnIndexes;
    }

    public String getString(String columnName) {
        return getString(columnName, 0);
    }
    public String getString(String columnName, int occurrence) {
        Cell cell = getCell(columnName, occurrence);

        if (cell == null) {
            return null;
        }

        return cell.toString().trim();
    }

    public Integer getInt(String columnName) {
        Double value = getDouble(columnName);

        if (value == null) {
            return null;
        }

        return value.intValue();
    }

    public Double getDouble(String columnName) {
        Cell cell = getCell(columnName);

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.STRING) {
            return parseNumeric(cell.getStringCellValue().trim(), columnName);
        }

        return cell.getNumericCellValue();
    }

    private Double parseNumeric(String value, String columnName) {
        if (value.isBlank()) {
            return null;
        }

        // Formats saisis à la main : séparateur décimal français, espaces de milliers
        // (y compris l'espace insécable), symboles éventuels.
        String normalized = value
            .replace(' ', ' ')
            .replace(" ", "")
            .replace(',', '.');

        try {
            return Double.valueOf(normalized);
        } catch (NumberFormatException e) {
            throw new ExcelParsingException("Valeur numérique invalide pour la colonne '" + columnName + "' : " + value);
        }
    }

    public LocalDate getDate(String columnName) {
        Cell cell = getCell(columnName);

        if (cell == null || cell.getCellType() == CellType.BLANK) 
        {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
        {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        String value = cell.toString().trim();
        try {
            return parseLatestDate(value);
        } catch (DateTimeParseException e) {
            throw new ExcelParsingException("Date invalide pour la colonne '" + columnName + "' : " + value, e);
        }
    }

    public Boolean getBoolean(String columnName) {
        Cell cell = getCell(columnName);

        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }

        String value = cell.toString().trim();

        if (value.equalsIgnoreCase("oui")) {
            return true;
        }

        if (value.equalsIgnoreCase("non")) {
            return false;
        }

        return null;
    }

    public boolean isEmpty(String columnName) {
        String value = getString(columnName);

        return value == null || value.isBlank();
    }

    public boolean hasColumn(String columnName) {
        return columnIndexes.containsKey(columnName);
    }

    /**
     * Retourne la première valeur non vide parmi les en-têtes candidats.
     * Utile pour les colonnes optionnelles (état d'avancement, identifiant traitement…).
     */
    public String getOptionalString(String... columnNames) {
        for (String columnName : columnNames) {
            if (hasColumn(columnName) && !isEmpty(columnName)) {
                return getString(columnName);
            }
        }
        return null;
    }

    public Integer getOptionalInt(String columnName) {
        if (!hasColumn(columnName) || isEmpty(columnName)) {
            return null;
        }
        try {
            return getInt(columnName);
        } catch (ExcelParsingException e) {
            return null;
        }
    }

    public Boolean getCroix(String columnName) {
        if (!hasColumn(columnName)) {
            return null;
        }
        if (isEmpty(columnName)) {
            return Boolean.FALSE;
        }
        String value = getString(columnName);
        if (value.equalsIgnoreCase("non") || value.equals("0")) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalDate parseLatestDate(String value) {
        return Arrays.stream(value.split("\\R"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> LocalDate.parse(s, DATE_FORMAT))
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    private Cell getCell(String columnName) {
        return getCell(columnName, 0);
    }

    private Cell getCell(String columnName, int occurrence) {
        List<Integer> indexes = columnIndexes.get(columnName);

        if (indexes == null) {
            throw new IllegalArgumentException("Unknown column: " + columnName);
        }

        if (occurrence < 0 || occurrence >= indexes.size()) {

        throw new IllegalArgumentException("Column '" + columnName +"' does not have occurrence " + occurrence);
        }

        return row.getCell(indexes.get(occurrence));
    }
}
