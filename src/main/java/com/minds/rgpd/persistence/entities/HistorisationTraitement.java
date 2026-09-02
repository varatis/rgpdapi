package com.minds.rgpd.persistence.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "historisation_Traitement")
@AllArgsConstructor
@NoArgsConstructor
public class HistorisationTraitement extends HistorisationGenerique {
    @NotNull
    @ManyToOne
    @JoinColumn(name = "traitement_uuid")
    Traitement traitement;

    /*public HistorisationTraitement(){
        super();
    }*/
}
