package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "CLIENT")
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "nom")
    @NotNull
    String nom;

    @Column(name = "statut")
    String statut;

    @Column(name = "version")
    String version;

    @Column(name = "date_version")
    LocalDate dateVersion;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    List<Duree> durees;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Definition> definitions;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    List<ResponsableTraitement> responsablesTraitement;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    List<HistorisationRegistre> historiqueTraitement;

}
