TRUNCATE TABLE traitement_etablissement, utilisateur_etablissement, profil, client, utilisateur, etablissement, traitement CASCADE;
-- ============================================
-- Données factices
-- ============================================

-- PROFILS
INSERT INTO profil (id, code, description)
VALUES (1, 'ADMIN', 'Administrateur du système'),
       (2, 'USER', 'Utilisateur standard'),
       (3, 'DPO', 'Data Protection Officer');

-- CLIENTS
INSERT INTO client (id, nom, statut)
VALUES (1, 'La breteche', 'actif'),
       (2, 'Entreprise Alpha', 'actif'),
       (3, 'Entreprise Beta', 'inactif');

-- UTILISATEURS
INSERT INTO utilisateur (id, prenom, nom, email, password, fonction, id_profil, id_client)
VALUES (1, 'Alice', 'Dupont', 'alice@alpha.com', 'hashedpwd1', 'Responsable IT', 1, 1),
       (2, 'Bob', 'Martin', 'bob@alpha.com', 'hashedpwd2', 'Employé', 2, 1),
       (3, 'Claire', 'Durand', 'claire@beta.com', 'hashedpwd3', 'DPO', 3, 2);

-- ETABLISSEMENTS
INSERT INTO etablissement (id, nom, id_client)
VALUES (1, 'Siège Paris', 1),
       (2, 'Agence Lyon', 1),
       (3, 'Siège Marseille', 2);

-- LIENS UTILISATEUR ↔ ETABLISSEMENT
INSERT INTO utilisateur_etablissement (id_utilisateur, id_etablissement)
VALUES (1, 1), -- Alice ↔ Siège Paris
       (2, 2), -- Bob ↔ Agence Lyon
       (3, 3);
-- Claire ↔ Siège Marseille

-- TRAITEMENTS
INSERT INTO traitement (id, nom, gestionnaire, finalite_principale, id_client)
VALUES (1, 'Gestion des salariés', 'Alice Dupont', 'Administration RH', 1),
       (2, 'Suivi des ventes', 'Bob Martin', 'Analyse commerciale', 1),
       (3, 'Conformité RGPD', 'Claire Durand', 'Protection des données', 2);

-- LIENS TRAITEMENT ↔ ETABLISSEMENT
INSERT INTO traitement_etablissement (id_traitement, id_etablissement)
VALUES (1, 1), -- Gestion salariés ↔ Siège Paris
       (2, 2), -- Suivi ventes ↔ Agence Lyon
       (3, 3); -- Conformité RGPD ↔ Siège Marseille
