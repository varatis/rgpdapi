package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * Définition rattachée à un client.
 * <p>
 * La colonne {@code type} sert de discriminateur : chaque sous-classe fixe sa
 * propre valeur via {@code @DiscriminatorValue} :
 * {@link EtudeImpact}, {@link FinalitePrincipale}, {@link LiceiteTraitement}
 * et {@link Sensibilite}.
 */
@Data
@Entity
@Table(name = "definition")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Definition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    /**
     * Valeur du discriminateur, exposée en lecture seule : elle est portée par
     * la sous-classe, d'où {@code insertable / updatable = false}.
     */
    @Column(name = "type", insertable = false, updatable = false)
    private String type;

    @NotNull
    @Column(name = "valeur")
    private String valeur;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    /**
     * Renseigne le discriminateur en memoire.
     * La colonne est ecrite par Hibernate lui-meme (insertable/updatable =
     * false) : sans cela, une definition tout juste creee exposerait un type
     * null tant qu elle n a pas ete rechargee depuis la base.
     */
    @PrePersist
    void initialiserType() {
        if (Objects.isNull(type)) {
            DiscriminatorValue discriminateur = getClass().getAnnotation(DiscriminatorValue.class);
            if (Objects.nonNull(discriminateur)) {
                type = discriminateur.value();
            }
        }
    }
}
