package com.minds.rgpd.persistence.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class HistorisationGenerique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotNull
    @Column(name = "date")
    private LocalDateTime date;

    @NotNull
    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    /**
     * Identifiant de l'utilisateur à l'origine de la modification (RG1).
     * Renseigné à partir du JWT lorsqu'un utilisateur est authentifié,
     * « import » lorsque la modification provient d'un import de fichier.
     */
    @Column(name = "auteur")
    private String auteur;
}
