-- ============================================
-- Migration de la table traitement (version clean)
-- ============================================

ALTER TABLE TRAITEMENT
    ADD COLUMN IF NOT EXISTS version INT,
    ADD COLUMN IF NOT EXISTS date_identification DATE NOT NULL,
    ADD COLUMN IF NOT EXISTS date_mise_a_jour DATE,
    ADD COLUMN IF NOT EXISTS historique_modifications VARCHAR (255),
    ADD COLUMN IF NOT EXISTS data_protection_officer VARCHAR (255),
    ADD COLUMN IF NOT EXISTS responsable_traitement VARCHAR (255),
    ADD COLUMN IF NOT EXISTS gestionnaire_mise_en_oeuvre VARCHAR (255),
    ADD COLUMN IF NOT EXISTS sous_finalites VARCHAR (255),
    ADD COLUMN IF NOT EXISTS categories_personnes_concernees VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_identification VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_connexion VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_localisation VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_comportement_vie_perso VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_economiques_financieres VARCHAR (255),
    ADD COLUMN IF NOT EXISTS donnees_professionnelles VARCHAR (255),
    ADD COLUMN IF NOT EXISTS categories_particulieres_donnees VARCHAR (255),
    ADD COLUMN IF NOT EXISTS sensibilite VARCHAR (255),
    ADD COLUMN IF NOT EXISTS etude_impact VARCHAR (255),
    ADD COLUMN IF NOT EXISTS canaux_collecte_donnees VARCHAR (255),
    ADD COLUMN IF NOT EXISTS liceite_traitement VARCHAR (255),
    ADD COLUMN IF NOT EXISTS recours_traitements_automatises BOOLEAN,
    ADD COLUMN IF NOT EXISTS emplacement_physique VARCHAR (255),
    ADD COLUMN IF NOT EXISTS dispositions_securite_donnees VARCHAR (255),
    ADD COLUMN IF NOT EXISTS emplacement_numerique VARCHAR (255),
    ADD COLUMN IF NOT EXISTS hebergement VARCHAR (255),
    ADD COLUMN IF NOT EXISTS duree_conservation INT,
    ADD COLUMN IF NOT EXISTS archivage BOOLEAN,
    ADD COLUMN IF NOT EXISTS duree_archivage INT,
    ADD COLUMN IF NOT EXISTS categories_destinataires VARCHAR (255),
    ADD COLUMN IF NOT EXISTS raisons_transfert_destinataires VARCHAR (255),
    ADD COLUMN IF NOT EXISTS transferts_hors_ue BOOLEAN,
    ADD COLUMN IF NOT EXISTS pays_destinataires VARCHAR (255),
    ADD COLUMN IF NOT EXISTS commentaires VARCHAR (255);

