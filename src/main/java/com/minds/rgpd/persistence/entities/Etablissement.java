package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@Table(name = "ETABLISSEMENT")
@NoArgsConstructor
@AllArgsConstructor
public class Etablissement {

    @Id
    Integer id;

    @Column(name = "nom")
    String nom;

    @ManyToOne
    @JoinColumn(name = "id_client")
    Client client;

}
