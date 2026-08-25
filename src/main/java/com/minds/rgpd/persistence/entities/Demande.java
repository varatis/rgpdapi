package com.minds.rgpd.persistence.entities;

import com.minds.rgpd.business.enums.DemandeStatut;
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
@Table(name = "demande")
@NoArgsConstructor
@AllArgsConstructor
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "type_demande")
    @NotNull
    String typeDemande;

    @Column(name = "description_synthetique")
    String descriptionSynthetique;

    @Column(name = "origine")
    String origine;

    @Column(name = "date_reception")
    LocalDate dateReception;

    @Column(name = "services_concernes")
    String servicesConcernes;

    @Column(name = "detail_traitement")
    String detailTraitement;

    @Column(name = "services_impliques")
    String servicesImpliques;

    @Column(name = "reponse")
    String reponse;

    @Column(name = "alerte_rt")
    String alerteRt;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    DemandeStatut statut;

    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;
}