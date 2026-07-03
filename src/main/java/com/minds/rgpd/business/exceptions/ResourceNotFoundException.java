package com.minds.rgpd.business.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String field, Object value) {
        super("%s introuvable avec %s = '%s'".formatted(resource, field, value));
    }
}