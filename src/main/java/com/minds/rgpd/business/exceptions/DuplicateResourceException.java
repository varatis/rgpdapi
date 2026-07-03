package com.minds.rgpd.business.exceptions;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String resource, String field, Object value) {
        super("%s existe déjà avec %s = '%s'".formatted(resource, field, value));
    }
}