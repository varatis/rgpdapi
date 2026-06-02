ALTER TABLE traitement
    ALTER COLUMN historique_modifications TYPE TEXT,
    ALTER COLUMN sous_finalites TYPE TEXT,
    ALTER COLUMN donnees_identification TYPE TEXT,
    ALTER COLUMN categories_particulieres_donnees TYPE TEXT,
    ALTER COLUMN emplacement_physique TYPE TEXT,
    ALTER COLUMN dispositions_securite_donnees TYPE TEXT,
    ALTER COLUMN categories_destinataires TYPE TEXT,
    ALTER COLUMN raisons_transfert_destinataires TYPE TEXT,
    ALTER COLUMN commentaires TYPE TEXT;