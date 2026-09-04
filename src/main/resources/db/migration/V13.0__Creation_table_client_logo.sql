-- Table CLIENT_LOGO (logo d'un client : au plus un par client)
--
-- Le contenu binaire est isolé dans sa propre table plutôt qu'ajouté en colonne
-- sur CLIENT : l'entité Client est chargée par la quasi-totalité des requêtes du
-- domaine (import de registre, liste des clients, ...) et un BYTEA porté par
-- CLIENT serait rapatrié à chaque SELECT.
--
-- client_id est à la fois clé primaire et clé étrangère : la relation 1-1 est
-- garantie par le schéma, et la suppression d'un client emporte son logo.
CREATE TABLE IF NOT EXISTS client_logo
(
    client_id    UUID         NOT NULL PRIMARY KEY,
    content      BYTEA        NOT NULL,
    content_type VARCHAR(64)  NOT NULL,
    -- Empreinte SHA-256 du contenu, utilisée comme ETag HTTP. Stockée (et non
    -- recalculée à la lecture) pour qu'une requête conditionnelle puisse
    -- répondre 304 sans jamais lire le BYTEA.
    etag         VARCHAR(64)  NOT NULL,
    -- Métadonnées d'affichage de l'écran de modification du logo.
    file_name    VARCHAR(255) NOT NULL,
    file_size    INTEGER      NOT NULL,
    CONSTRAINT fk_client_logo_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE
);
