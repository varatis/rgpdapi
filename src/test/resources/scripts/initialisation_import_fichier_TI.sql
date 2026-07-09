TRUNCATE TABLE traitement_etablissement, utilisateur_etablissement, profil, client, utilisateur, etablissement, traitement CASCADE;
-- ============================================
-- Données factices
-- ============================================

-- PROFILS
INSERT INTO profil (id, code, description)
VALUES ('ff2b79c3-c2a6-4f7f-b3dc-f78e090ba8f9', 'ADMIN', 'Administrateur du système'),
       ('b4d32179-04d2-44e6-9f91-524e4dc1818a', 'USER', 'Utilisateur standard'),
       ('32d2807f-2540-443e-8f85-ffc6975e746f', 'DPO', 'Data Protection Officer');

-- CLIENTS
INSERT INTO client (id, nom, statut)
VALUES ('0e4bf889-fea0-46ac-894d-ca39cbf00359', 'La breteche', 'actif'),
       ('82e99259-1bbd-4c1a-b013-7602e27168f3', 'Entreprise Alpha', 'actif'),
       ('e0aa8d87-69ca-4e64-9c39-d47aecaf38ed', 'Entreprise Beta', 'inactif');

-- UTILISATEURS
INSERT INTO utilisateur (id, prenom, nom, email, password, fonction, id_profil, id_client)
VALUES ('d6dfd117-8047-4a9a-afca-f5268a38bfcf', 'Alice', 'Dupont', 'alice@alpha.com', 'hashedpwd1', 'Responsable IT', 'ff2b79c3-c2a6-4f7f-b3dc-f78e090ba8f9', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('6a04222b-60f8-434b-bdff-c01ce36fde2f', 'Bob', 'Martin', 'bob@alpha.com', 'hashedpwd2', 'Employé', 'b4d32179-04d2-44e6-9f91-524e4dc1818a', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('e9048a22-e73d-4b35-b08a-0540c58e7a6f', 'Claire', 'Durand', 'claire@beta.com', 'hashedpwd3', 'DPO', '32d2807f-2540-443e-8f85-ffc6975e746f', '82e99259-1bbd-4c1a-b013-7602e27168f3');

-- ETABLISSEMENTS
INSERT INTO etablissement (id, nom, id_client)
VALUES ('590687e6-f6e9-4668-aae0-a0f0e32982ff', 'Siège Paris', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('bf10040d-c95f-42bb-8298-514fe45d84c1', 'Agence Lyon', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('cb5b0cd7-8551-4808-b3a2-fc5e745b25d2', 'Siège Marseille', '82e99259-1bbd-4c1a-b013-7602e27168f3');

-- LIENS UTILISATEUR ↔ ETABLISSEMENT
INSERT INTO utilisateur_etablissement (id_utilisateur, id_etablissement)
VALUES ('d6dfd117-8047-4a9a-afca-f5268a38bfcf', '590687e6-f6e9-4668-aae0-a0f0e32982ff'), -- Alice ↔ Siège Paris
       ('6a04222b-60f8-434b-bdff-c01ce36fde2f', 'bf10040d-c95f-42bb-8298-514fe45d84c1'), -- Bob ↔ Agence Lyon
       ('e9048a22-e73d-4b35-b08a-0540c58e7a6f', 'cb5b0cd7-8551-4808-b3a2-fc5e745b25d2');
-- Claire ↔ Siège Marseille

-- TRAITEMENTS
INSERT INTO traitement (identifiant, id_fonctionnel, nom, gestionnaire_mise_en_oeuvre, date_identification, finalite_principale, id_client)
VALUES ('31b8d234-2346-4761-89fb-92d24f49bb96', 1,'Gestion des salariés', 'Alice Dupont', '2026-01-01' ,'Administration RH', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('acc27fd1-58ea-4333-b300-b4a32bef6a63', 2, 'Suivi des ventes', 'Bob Martin', '2026-01-01','Analyse commerciale', '0e4bf889-fea0-46ac-894d-ca39cbf00359'),
       ('5aed6dd1-d164-41f7-a786-f1be625333ae', 3, 'Conformité RGPD', 'Claire Durand','2026-01-01' ,'Protection des données', '82e99259-1bbd-4c1a-b013-7602e27168f3');

-- LIENS TRAITEMENT ↔ ETABLISSEMENT
INSERT INTO traitement_etablissement (id_traitement, id_etablissement)
VALUES ('31b8d234-2346-4761-89fb-92d24f49bb96', '590687e6-f6e9-4668-aae0-a0f0e32982ff'), -- Gestion salariés ↔ Siège Paris
       ('acc27fd1-58ea-4333-b300-b4a32bef6a63', 'bf10040d-c95f-42bb-8298-514fe45d84c1'), -- Suivi ventes ↔ Agence Lyon
       ('5aed6dd1-d164-41f7-a786-f1be625333ae', 'cb5b0cd7-8551-4808-b3a2-fc5e745b25d2'); -- Conformité RGPD ↔ Siège Marseille
