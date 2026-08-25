package com.minds.rgpd.business.Imports;

import java.util.List;
import java.util.stream.Collectors;

public record ImportResult<T>(List<T> imported, List<ImportError> errors) 
{
    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    public String getResultMessage() {
        if (errors.isEmpty()) {
            return "OK";
        }

        return errors.stream()
            .map(error -> "Ligne " + error.rowNumber() + ": " + error.message())
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
