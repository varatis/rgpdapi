package com.minds.rgpd.persistence.entities;

import com.minds.rgpd.business.enums.ViolationStatut;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@Entity
@Table(name = "violation")
@NoArgsConstructor
@AllArgsConstructor
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID identifiant;

    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    @Column(name = "date_violation")
    LocalDate dateViolation;

    @Column(name = "nature_violation")
    String natureViolation;

    @Column(name = "donnees_concernees")
    String donneesConcernees;

    @Column(name = "nombre_approximatif_donnees_concernees")
    Integer nombreApproximatifDonneesConcernees;

    @Column(name = "categories_personnes_concernees")
    String categoriesPersonnesConcernees;

    @Column(name = "nombre_personnes_concernees")
    Integer nombrePersonnesConcernees;

    @Column(name = "consequences")
    String consequences;

    @Column(name = "mesures_prises_prevues")
    String mesuresPrisesPrevues;

    @Column(name = "information_cnil")
    String informationCnil;

    @Column(name = "risque_eleve_droits_libertes")
    Boolean risqueEleveDroitsLibertes;

    @Column(name = "communication_personnes_effectuee_et_date")
    String communicationPersonnesEffectueeEtDate;

    @Column(name = "commentaires")
    String commentaires;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    @NotNull
    ViolationStatut statut;

}
