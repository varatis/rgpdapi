package com.minds.rgpd.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * Logo d'un client, stocké en base plutôt que sur le disque : le répertoire
 * d'upload du pod est éphémère (aucun volume persistant monté), il ne survivrait
 * ni à un redémarrage ni à une montée à plusieurs réplicas.
 * <p>
 * Aucune relation inverse n'est déclarée sur {@link Client} : le logo se charge
 * uniquement par son identifiant client, ce qui garantit qu'aucune requête du
 * domaine ne rapatrie le contenu binaire par inadvertance.
 */
@Builder
@Data
@Entity
@Table(name = "client_logo")
@NoArgsConstructor
@AllArgsConstructor
public class ClientLogo {

    @Id
    @Column(name = "client_id")
    UUID clientId;

    /** @MapsId : l'identifiant de l'entité est celui du client associé. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    @NotNull
    @ToString.Exclude
    Client client;

    @ToString.Exclude
    @Column(name = "content", columnDefinition = "bytea", nullable = false)
    byte[] content;

    @Column(name = "content_type", nullable = false, length = 64)
    String contentType;

    /** Empreinte SHA-256 du contenu, exposée telle quelle comme ETag HTTP. */
    @Column(name = "etag", nullable = false, length = 64)
    String etag;

    /**
     * Nom du fichier tel que fourni par le navigateur : donnée non fiable,
     * conservée uniquement pour l'affichage de l'écran de modification. Ne doit
     * jamais être réutilisée dans un en-tête HTTP ni dans un chemin de fichier.
     */
    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;

    @Column(name = "file_size", nullable = false)
    Integer fileSize;
}
