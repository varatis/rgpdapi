package com.minds.rgpd.persistence.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/** Définition de type « Liceité Traitement ». */
@Entity
@DiscriminatorValue(LiceiteTraitement.TYPE)
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LiceiteTraitement extends Definition {

    public static final String TYPE = "Liceité Traitement";
}
