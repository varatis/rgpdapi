-- Suivi des préconisations associées aux traitements.
-- Référence fonctionnelle : onglet Excel "Préconisations" / "Suivi des préconisations"
-- (libellé, explication, risque, contraintes, coût, priorité, complexité, commentaire).
-- etat_avancement n'existe pas dans le catalogue Excel : colonne de suivi, texte libre
-- (ex. "À faire", "En cours", "Réalisée", "Non applicable") si fournie à l'import.

CREATE TABLE preconisation
(
    identifiant       UUID         NOT NULL PRIMARY KEY,
    id_client         UUID         NOT NULL,
    id_traitement     UUID,
    libelle           VARCHAR(255) NOT NULL,
    explication       TEXT,
    risque_encours    TEXT,
    contraintes       TEXT,
    cout              TEXT,
    priorite          VARCHAR(255),
    complexite        VARCHAR(255),
    commentaire       TEXT,
    etat_avancement   VARCHAR(100),
    CONSTRAINT fk_preconisation_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE,
    CONSTRAINT fk_preconisation_traitement FOREIGN KEY (id_traitement) REFERENCES traitement (identifiant) ON DELETE CASCADE
);

CREATE INDEX idx_preconisation_client ON preconisation (id_client);
CREATE INDEX idx_preconisation_traitement ON preconisation (id_traitement);
CREATE INDEX idx_preconisation_etat ON preconisation (etat_avancement);
