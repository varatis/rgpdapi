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
@Table(name = "UTILISATEUR")
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    Integer id;

    @Column(name = "prenom")
    @NotNull
    String prenom;

    @Column(name = "nom")
    @NotNull
    String nom;

    @Column(name = "email")
    @NotNull
    String email;

    @Column(name = "password")
    @NotNull
    String password;

    @Column(name = "fonction")
    String fonction;

    @ManyToOne
    @JoinColumn(name = "id_profil")
    Profil profil;

    @ManyToOne
    @JoinColumn(name = "id_client")
    Client client;

}
