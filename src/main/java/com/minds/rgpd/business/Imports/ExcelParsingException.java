package com.minds.rgpd.business.Imports;

public class ExcelParsingException extends RuntimeException {

    public ExcelParsingException(String message) {
        super(message);
    }

    public ExcelParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
