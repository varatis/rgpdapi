-- Colonnes de versioning du registre (entité Client)
ALTER TABLE client
    ADD COLUMN IF NOT EXISTS version      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS date_version DATE;

-- Table DEFINITION (entité Definition)
CREATE TABLE definition
(
    id        SERIAL PRIMARY KEY,
    type      VARCHAR(255) NOT NULL,
    valeur    VARCHAR(255) NOT NULL,
    client_id UUID,
    CONSTRAINT fk_definitions_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE
);

-- Table DUREE (entité Duree)
CREATE TABLE duree
(
    id            SERIAL PRIMARY KEY,
    est_archivage BOOLEAN      NOT NULL,
    valeur        VARCHAR(255) NOT NULL,
    client_id     UUID,
    CONSTRAINT fk_duree_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE,
    -- Le resolveur applicatif s'appuie sur cette unicite pour reutiliser une duree
    -- existante plutot que de la dupliquer a chaque ligne importee.
    CONSTRAINT uq_duree_client_est_archivage_valeur UNIQUE (client_id, est_archivage, valeur)
);

-- Table RESPONSABLES_TRAITEMENT (entité ResponsableTraitement, relation 1-1 avec Client)
CREATE TABLE responsables_traitement
(
    id                           SERIAL PRIMARY KEY,
    client_id                    UUID         NOT NULL,
    valeur                       VARCHAR(255) NOT NULL,
    informations_complementaires TEXT,
    CONSTRAINT fk_responsable_client FOREIGN KEY (client_id) REFERENCES client (id) ON DELETE CASCADE,
    CONSTRAINT uq_responsable_client_valeur UNIQUE (client_id, valeur)
);

-- Table HISTORISATION_REGISTRE (entité HistorisationRegistre)
CREATE TABLE historisation_registre
(
    id          SERIAL PRIMARY KEY,
    date        TIMESTAMP    NOT NULL,
    motif       VARCHAR(255) NOT NULL,
    client_uuid UUID         NOT NULL,
    CONSTRAINT fk_historisation_registre_client FOREIGN KEY (client_uuid) REFERENCES client (id) ON DELETE CASCADE
);

-- Table HISTORISATION_TRAITEMENT (entité HistorisationTraitement)
CREATE TABLE historisation_traitement
(
    id              SERIAL PRIMARY KEY,
    date            TIMESTAMP    NOT NULL,
    motif           VARCHAR(255) NOT NULL,
    traitement_uuid UUID         NOT NULL,
    CONSTRAINT fk_historisation_traitement_traitement FOREIGN KEY (traitement_uuid) REFERENCES traitement (identifiant) ON DELETE CASCADE
);


-- Traitement : les 4 champs correspondant aux types de définition deviennent des références vers la table definition
ALTER TABLE traitement
    DROP COLUMN IF EXISTS finalite_principale,
    DROP COLUMN IF EXISTS sensibilite,
    DROP COLUMN IF EXISTS etude_impact,
    DROP COLUMN IF EXISTS liceite_traitement;

ALTER TABLE traitement
    ADD COLUMN finalite_principale_id INTEGER,
    ADD COLUMN sensibilite_id         INTEGER,
    ADD COLUMN etude_impact_id        INTEGER,
    ADD COLUMN liceite_traitement_id  INTEGER,
    ADD CONSTRAINT fk_traitement_finalite_principale FOREIGN KEY (finalite_principale_id) REFERENCES definition (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_traitement_sensibilite FOREIGN KEY (sensibilite_id) REFERENCES definition (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_traitement_etude_impact FOREIGN KEY (etude_impact_id) REFERENCES definition (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_traitement_liceite_traitement FOREIGN KEY (liceite_traitement_id) REFERENCES definition (id) ON DELETE SET NULL;

-- Traitement : les durées de conservation et d'archivage deviennent des références vers la table duree
ALTER TABLE traitement
    DROP COLUMN IF EXISTS duree_conservation,
    DROP COLUMN IF EXISTS duree_archivage;

ALTER TABLE traitement
    ADD COLUMN duree_conservation_id INTEGER,
    ADD COLUMN duree_archivage_id    INTEGER,
    ADD CONSTRAINT fk_traitement_duree_conservation FOREIGN KEY (duree_conservation_id) REFERENCES duree (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_traitement_duree_archivage FOREIGN KEY (duree_archivage_id) REFERENCES duree (id) ON DELETE SET NULL;

-- Traitement : le responsable de traitement devient une référence vers la table responsables_traitement
ALTER TABLE traitement
    DROP COLUMN IF EXISTS responsable_traitement;

ALTER TABLE traitement
    ADD COLUMN responsable_traitement_id INTEGER,
    ADD CONSTRAINT fk_traitement_responsable_traitement FOREIGN KEY (responsable_traitement_id) REFERENCES responsables_traitement (id) ON DELETE SET NULL;
