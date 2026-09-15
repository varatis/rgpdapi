package com.minds.rgpd.business.exceptions;

/**
 * Ressource encore referencee ailleurs : la supprimer effacerait aussi les
 * liens qui la citent.
 */
public class ResourceInUseException extends RuntimeException {
    public ResourceInUseException(String resource, Object value, String usage) {
        super("%s '%s' est rattaché à %s : suppression impossible".formatted(resource, value, usage));
    }
}
