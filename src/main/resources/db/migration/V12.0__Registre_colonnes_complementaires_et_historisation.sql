-- =====================================================================================
-- Finalisation du registre de traitements
--   RG1 : toute modification d'un traitement est historisée (auteur + motif)
--   RG4 : la version du registre est portée par le client (déjà présent) et alimentée
--         par l'import ; on trace la version dans l'historique du registre
--   RG5 : colonnes complémentaires du registre (bloc « Analyse de conformité » et
--         « Critères PIA ») désormais importables
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- RG1 : historisation
-- -------------------------------------------------------------------------------------
ALTER TABLE historisation_traitement
    ALTER COLUMN motif TYPE TEXT;

ALTER TABLE historisation_traitement
    ADD COLUMN IF NOT EXISTS auteur VARCHAR(255);

ALTER TABLE historisation_registre
    ALTER COLUMN motif TYPE TEXT;

ALTER TABLE historisation_registre
    ADD COLUMN IF NOT EXISTS auteur VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_historisation_traitement_traitement
    ON historisation_traitement (traitement_uuid);
CREATE INDEX IF NOT EXISTS idx_historisation_registre_client
    ON historisation_registre (client_uuid);

-- -------------------------------------------------------------------------------------
-- RG5 : colonnes complémentaires du registre de traitements
--       (onglet « Registre de traitement », colonnes AN à BT du modèle CREATIVE)
-- -------------------------------------------------------------------------------------
ALTER TABLE traitement
    ADD COLUMN IF NOT EXISTS impact_traitement                    INTEGER,
    ADD COLUMN IF NOT EXISTS detournement_finalite                INTEGER,
    ADD COLUMN IF NOT EXISTS score_detournement_finalite          INTEGER,
    ADD COLUMN IF NOT EXISTS collecte_dcp_inappropriees           INTEGER,
    ADD COLUMN IF NOT EXISTS score_collecte_dcp_inappropriees     INTEGER,
    ADD COLUMN IF NOT EXISTS conservation_excessive_dcp           INTEGER,
    ADD COLUMN IF NOT EXISTS score_conservation_excessive_dcp     INTEGER,
    ADD COLUMN IF NOT EXISTS securisation_insuffisante_dcp        INTEGER,
    ADD COLUMN IF NOT EXISTS score_securisation_insuffisante_dcp  INTEGER,
    ADD COLUMN IF NOT EXISTS vices_consentement                   INTEGER,
    ADD COLUMN IF NOT EXISTS score_vices_consentement             INTEGER,
    ADD COLUMN IF NOT EXISTS manque_transparence                  INTEGER,
    ADD COLUMN IF NOT EXISTS score_manque_transparence            INTEGER,
    ADD COLUMN IF NOT EXISTS incapacite_exercice_droits           INTEGER,
    ADD COLUMN IF NOT EXISTS score_incapacite_exercice_droits     INTEGER,
    ADD COLUMN IF NOT EXISTS transfert_tiers_mal_encadre          INTEGER,
    ADD COLUMN IF NOT EXISTS score_transfert_tiers_mal_encadre    INTEGER,
    ADD COLUMN IF NOT EXISTS transfert_hors_ue_abusif             INTEGER,
    ADD COLUMN IF NOT EXISTS score_transfert_hors_ue_abusif       INTEGER,
    ADD COLUMN IF NOT EXISTS defaut_preuve                        INTEGER,
    ADD COLUMN IF NOT EXISTS score_defaut_preuve                  INTEGER,
    ADD COLUMN IF NOT EXISTS score_global                         INTEGER,
    ADD COLUMN IF NOT EXISTS commentaires_analyse                 TEXT,
    ADD COLUMN IF NOT EXISTS exposition_traitement                INTEGER;

-- Critères PIA : cochés d'une croix dans le fichier Excel, stockés en booléen.
ALTER TABLE traitement
    ADD COLUMN IF NOT EXISTS critere_evaluation_scoring           BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_decision_automatique         BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_surveillance_systematique    BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_collecte_donnees_sensibles   BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_collecte_large_echelle       BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_croisement_donnees           BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_personnes_vulnerables        BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_usage_innovant               BOOLEAN,
    ADD COLUMN IF NOT EXISTS critere_exclusion_benefice_droit     BOOLEAN;
