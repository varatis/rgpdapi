package com.minds.rgpd.business.exceptions;

import lombok.Getter;

@Getter
public class IdentityProviderException extends RuntimeException {

    private final int statut;
    private final String operation;

    public IdentityProviderException(String operation, int statut, String detail) {
        super("Keycloak indisponible ou en erreur sur l'opération '%s' (statut %s) : %s"
                .formatted(operation, statut, detail));
        this.operation = operation;
        this.statut = statut;
    }

    public IdentityProviderException(String operation, String detail, Throwable cause) {
        super("Keycloak injoignable sur l'opération '%s' : %s".formatted(operation, detail), cause);
        this.operation = operation;
        this.statut = -1;
    }

    public boolean conflit() {
        return statut == 409;
    }

    public boolean introuvable() {
        return statut == 404;
    }
}
