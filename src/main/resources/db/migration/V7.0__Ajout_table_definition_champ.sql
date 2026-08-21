-- ============================================================================
-- V7.0 - Table de stockage des définitions métier des champs du registre
-- ----------------------------------------------------------------------------
-- Les définitions métier de l'onglet « FR_Définitions » du fichier registre
-- ne sont PAS codées en dur dans les migrations : elles sont extraites du
-- fichier Excel à chaque import et persistées dans cette table
-- (voir DefinitionsRegistreImportService).
--
-- Elles peuvent ainsi être consultées en base (pgAdmin/DBeaver, API) et
-- réinjectées dans l'onglet « FR_Définitions » lors d'un futur export.
--
-- Correspondance libellé -> colonne BDD : docs/mapping-bdd-registre.md
-- ============================================================================

CREATE TABLE definition_champ
(
    id            UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    id_client     UUID         NOT NULL,
    edition       VARCHAR(20)  NOT NULL,
    section       VARCHAR(255),
    libelle       VARCHAR(255) NOT NULL,
    definition    TEXT         NOT NULL,
    table_cible   VARCHAR(100),
    colonne_cible VARCHAR(100),
    ordre         INT          NOT NULL,
    CONSTRAINT fk_definition_champ_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE,
    CONSTRAINT uq_definition_champ UNIQUE (id_client, edition, libelle, colonne_cible)
);

CREATE INDEX idx_definition_champ_client ON definition_champ (id_client);

-- Documentation technique de la structure (les définitions métier sont des
-- DONNÉES importées, elles ne figurent pas dans les migrations).
COMMENT ON TABLE definition_champ IS
    'Définitions métier des champs du registre des activités de traitement, extraites de l''onglet « FR_Définitions » du fichier Excel à chaque import (une version par client et édition).';
COMMENT ON COLUMN definition_champ.id IS 'Identifiant technique de la définition (UUID généré par la base).';
COMMENT ON COLUMN definition_champ.id_client IS 'Client (organisme) propriétaire du fichier registre importé (suppression en cascade).';
COMMENT ON COLUMN definition_champ.edition IS 'Édition du fichier registre source, extraite du nom de fichier (« ..._Registre RGPD_ed<édition>.xlsx »).';
COMMENT ON COLUMN definition_champ.section IS 'Section de l''onglet FR_Définitions regroupant le champ (ex. : Identification du traitement, Données personnelles traitées, Description du traitement).';
COMMENT ON COLUMN definition_champ.libelle IS 'Libellé du champ tel qu''écrit dans l''onglet FR_Définitions (ex. : « Nom du traitement »).';
COMMENT ON COLUMN definition_champ.definition IS 'Définition métier du champ, texte extrait de l''onglet FR_Définitions.';
COMMENT ON COLUMN definition_champ.table_cible IS 'Table du modèle de données correspondant au champ, si elle existe (NULL pour les définitions sans correspondance).';
COMMENT ON COLUMN definition_champ.colonne_cible IS 'Colonne du modèle de données correspondant au champ, si elle existe (NULL pour les définitions sans correspondance ou portant sur une relation).';
COMMENT ON COLUMN definition_champ.ordre IS 'Numéro de ligne du champ dans l''onglet FR_Définitions (conserve l''ordre d''affichage, utile pour régénérer l''onglet à l''export).';
