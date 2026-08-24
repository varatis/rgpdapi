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
@Table(name = "responsables_traitement")
@NoArgsConstructor
@AllArgsConstructor
public class ResponsableTraitement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @NotNull
    @Column(name = "valeur")
    private String valeur;

    @Column(name = "informations_complementaires")
    private String informationsComplementaires;
}
