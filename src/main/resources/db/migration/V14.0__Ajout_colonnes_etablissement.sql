-- Ajout des colonnes departement et principal sur la table ETABLISSEMENT
ALTER TABLE etablissement
    ADD COLUMN IF NOT EXISTS departement VARCHAR(3),
    ADD COLUMN IF NOT EXISTS principal BOOLEAN NOT NULL DEFAULT FALSE;

-- Ajout d'une contrainte d'unicité sur (nom, id_client)
ALTER TABLE etablissement
    ADD CONSTRAINT uq_etablissement_nom_client UNIQUE (nom, id_client);
