package com.minds.rgpd.persistence.repositories;

import com.minds.rgpd.persistence.entities.ClientLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientLogoRepository extends JpaRepository<ClientLogo, UUID> {

    /**
     * Projection fermée : Spring Data ne sélectionne que les colonnes déclarées
     * par {@link ClientLogoInfo}, le BYTEA n'est donc jamais lu. C'est ce qui
     * rend l'écran de modification et les requêtes conditionnelles peu coûteux.
     */
    Optional<ClientLogoInfo> findProjectedByClientId(UUID clientId);

    /** Lecture du seul ETag, pour répondre 304 sans charger le contenu. */
    Optional<ClientLogoEtag> findEtagByClientId(UUID clientId);

    boolean existsByClientId(UUID clientId);

    interface ClientLogoInfo {
        String getFileName();

        Integer getFileSize();

        String getContentType();

        String getEtag();
    }

    interface ClientLogoEtag {
        String getEtag();
    }
}
