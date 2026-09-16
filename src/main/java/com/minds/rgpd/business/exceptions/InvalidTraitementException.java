package com.minds.rgpd.business.exceptions;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class InvalidTraitementException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidTraitementException(Map<String, String> errors) {
        super("Traitement invalide : " + errors);
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }
}
