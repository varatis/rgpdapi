package com.minds.rgpd.business.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class IdentityProviderIndisponibleException extends RuntimeException {

    public IdentityProviderIndisponibleException(String message) {
        super(message);
    }
}