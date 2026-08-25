package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "preconisation")
@NoArgsConstructor
@AllArgsConstructor
public class Preconisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID identifiant;

    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    @ManyToOne
    @JoinColumn(name = "id_traitement")
    Traitement traitement;

    @Column(name = "libelle")
    @NotNull
    String libelle;

    @Column(name = "explication")
    String explication;

    @Column(name = "risque_encours")
    String risqueEncours;

    @Column(name = "contraintes")
    String contraintes;

    @Column(name = "cout")
    String cout;

    @Column(name = "priorite")
    String priorite;

    @Column(name = "complexite")
    String complexite;

    @Column(name = "commentaire")
    String commentaire;

    @Column(name = "etat_avancement")
    String etatAvancement;
}