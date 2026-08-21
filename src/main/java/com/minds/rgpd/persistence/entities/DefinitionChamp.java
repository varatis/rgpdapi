package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Définition métier d'un champ du registre des activités de traitement.
 * <p>
 * Les occurrences de cette entité ne sont pas codées en dur dans l'application :
 * elles sont extraites de l'onglet « FR_Définitions » du fichier Excel à chaque
 * import (une version par client et édition), puis peuvent être réinjectées dans
 * l'onglet lors d'un export.
 * <p>
 * Correspondance libellé → colonne BDD : {@code docs/mapping-bdd-registre.md}.
 */
@Builder
@Data
@Entity
@Table(name = "definition_champ")
@NoArgsConstructor
@AllArgsConstructor
public class DefinitionChamp {

    /**
     * Identifiant technique de la définition (UUID généré par l'application).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    /**
     * Client (organisme) propriétaire du fichier registre importé.
     */
    @ManyToOne
    @JoinColumn(name = "id_client")
    @NotNull
    Client client;

    /**
     * Édition du fichier registre source, extraite du nom de fichier
     * (« …_Registre RGPD_ed&lt;édition&gt;.xlsx »).
     */
    @Column(name = "edition")
    @NotNull
    String edition;

    /**
     * Section de l'onglet FR_Définitions regroupant le champ
     * (ex. : « Identification du traitement », « Données personnelles traitées »,
     * « Description du traitement »).
     */
    @Column(name = "section")
    String section;

    /**
     * Libellé du champ tel qu'écrit dans l'onglet FR_Définitions (ex. : « Nom du traitement »).
     */
    @Column(name = "libelle")
    @NotNull
    String libelle;

    /**
     * Définition métier du champ, texte extrait de l'onglet FR_Définitions.
     */
    @Column(name = "definition")
    @NotNull
    String definition;

    /**
     * Table du modèle de données correspondant au champ, si elle existe
     * (nulle pour les définitions sans correspondance).
     */
    @Column(name = "table_cible")
    String tableCible;

    /**
     * Colonne du modèle de données correspondant au champ, si elle existe
     * (nulle pour les définitions sans correspondance ou portant sur une relation).
     */
    @Column(name = "colonne_cible")
    String colonneCible;

    /**
     * Numéro de ligne du champ dans l'onglet FR_Définitions ; conserve l'ordre
     * d'affichage et permet de régénérer l'onglet à l'export.
     */
    @Column(name = "ordre")
    @NotNull
    Integer ordre;
}
