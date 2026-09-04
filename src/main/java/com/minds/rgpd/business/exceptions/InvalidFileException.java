package com.minds.rgpd.business.exceptions;

/** Fichier reçu inexploitable : format non accepté, vide, ou hors limites. */
public class InvalidFileException extends RuntimeException {
    public InvalidFileException(String message) {
        super(message);
    }
}
