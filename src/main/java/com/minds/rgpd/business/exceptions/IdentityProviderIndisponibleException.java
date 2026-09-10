package com.minds.rgpd.business.exceptions;

public class IdentityProviderIndisponibleException extends RuntimeException {

    public IdentityProviderIndisponibleException(String operation) {
        super("La synchronisation Keycloak est désactivée : opération '%s' impossible".formatted(operation));
    }
}
