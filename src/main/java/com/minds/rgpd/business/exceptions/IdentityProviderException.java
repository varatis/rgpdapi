package com.minds.rgpd.business.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class IdentityProviderException extends RuntimeException {

    private final HttpStatus statut;

    public IdentityProviderException(String action, String champ, String valeur) {
        super("Échec de la opération Keycloak : " + action + " sur le champ " + champ + " = " + valeur);
        this.statut = HttpStatus.BAD_GATEWAY;
    }

    /** Message libre, pour les échecs techniques (jeton inaccessible, réponse invalide…). */
    public IdentityProviderException(String message) {
        super(message);
        this.statut = HttpStatus.BAD_GATEWAY;
    }

    /**
     * Message libre avec cause préservée : permet aux appelants de distinguer
     * les échecs Keycloak par leur statut (ex. 409 doublon d'identifiant).
     */
    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
        this.statut = HttpStatus.BAD_GATEWAY;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}