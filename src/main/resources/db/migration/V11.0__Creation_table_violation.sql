-- Table VIOLATION (recueil des violations de données à caractère personnel)
CREATE TABLE IF NOT EXISTS violation
(
    identifiant                            UUID NOT NULL PRIMARY KEY,
    id_client                              UUID NOT NULL,
    date_violation                         DATE,
    nature_violation                       TEXT,
    donnees_concernees                     TEXT,
    nombre_approximatif_donnees_concernees INTEGER,
    categories_personnes_concernees        TEXT,
    nombre_personnes_concernees            INTEGER,
    consequences                           TEXT,
    mesures_prises_prevues                 TEXT,
    information_cnil                       TEXT,
    risque_eleve_droits_libertes           BOOLEAN,
    communication_personnes_effectuee_et_date      TEXT,
    commentaires                           TEXT,
    statut                                 VARCHAR(50) NOT NULL DEFAULT 'EN_COURS',
    CONSTRAINT fk_violation_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE
);
