package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@Entity
@Table(name = "duree")
@NoArgsConstructor
@AllArgsConstructor
public class Duree {

    public static final boolean CONSERVATION = false;
    public static final boolean ARCHIVAGE = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotNull
    @Column(name = "est_archivage")
    private boolean estArchivage;

    @NotNull
    @Column(name = "valeur")
    private String valeur;

    @ManyToOne
    @JoinColumn(name = "client_id")
    Client client;
}
