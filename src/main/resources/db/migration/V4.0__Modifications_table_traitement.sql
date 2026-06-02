ALTER TABLE traitement
    RENAME COLUMN dispositions_securite_donnees TO dispositions_securite_donnees_physique;
ALTER TABLE traitement
    ADD COLUMN IF NOT EXISTS dispositions_securite_donnees_numerique TEXT;
ALTER TABLE traitement
    ALTER COLUMN emplacement_numerique TYPE TEXT;
ALTER TABLE traitement
    ALTER COLUMN duree_conservation TYPE VARCHAR(255);
ALTER TABLE traitement
    ALTER COLUMN duree_archivage TYPE VARCHAR(255);