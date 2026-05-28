package com.minds.rgpd.business.utilities;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter(AccessLevel.PRIVATE)
public enum StatusFichierEnum {

    EN_COURS("En cours"),
    OK("Ok"),
    KO("Ko");

    public final String value;
}

