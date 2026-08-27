-- RG1 : historisation des modifications des préconisations.
-- Miroir de la table historisation_traitement (cf. V7) : à chaque modification
-- d'une préconisation, une ligne est écrite avec la date et le motif saisi
-- par l'utilisateur (CA4).

CREATE TABLE historisation_preconisation
(
    id                 SERIAL PRIMARY KEY,
    date               TIMESTAMP    NOT NULL,
    motif              VARCHAR(255) NOT NULL,
    preconisation_uuid UUID         NOT NULL,
    CONSTRAINT fk_historisation_preconisation_preconisation FOREIGN KEY (preconisation_uuid) REFERENCES preconisation (identifiant) ON DELETE CASCADE
);

CREATE INDEX idx_historisation_preconisation_preco ON historisation_preconisation (preconisation_uuid);
