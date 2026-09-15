package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "ETABLISSEMENT",
        uniqueConstraints = @UniqueConstraint(name = "uq_etablissement_nom_client", columnNames = {"nom", "id_client"}))
@NoArgsConstructor
@AllArgsConstructor
public class Etablissement {

    @Id
    UUID id;

    @Column(name = "nom")
    String nom;

    @Column(name = "departement", length = 3)
    String departement;

    @Column(name = "principal", nullable = false)
    boolean principal;

    @ManyToOne
    @JoinColumn(name = "id_client")
    Client client;
}
