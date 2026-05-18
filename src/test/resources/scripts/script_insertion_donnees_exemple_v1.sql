-- ============================================
-- Données factices
-- ============================================

-- PROFILS
INSERT INTO profil (code, description) VALUES
('ADMIN', 'Administrateur du système'),
('USER', 'Utilisateur standard'),
('DPO', 'Data Protection Officer');

-- CLIENTS
INSERT INTO client (nom, statut) VALUES
('Entreprise Alpha', 'actif'),
('Entreprise Beta', 'inactif');

-- UTILISATEURS
INSERT INTO utilisateur (prenom, nom, email, password, fonction, id_profil, id_client) VALUES
('Alice', 'Dupont', 'alice@alpha.com', 'hashedpwd1', 'Responsable IT', 1, 1),
('Bob', 'Martin', 'bob@alpha.com', 'hashedpwd2', 'Employé', 2, 1),
('Claire', 'Durand', 'claire@beta.com', 'hashedpwd3', 'DPO', 3, 2);

-- ETABLISSEMENTS
INSERT INTO etablissement (nom, id_client) VALUES
('Siège Paris', 1),
('Agence Lyon', 1),
('Siège Marseille', 2);

-- LIENS UTILISATEUR ↔ ETABLISSEMENT
INSERT INTO utilisateur_etablissement (id_utilisateur, id_etablissement) VALUES
(1, 1), -- Alice ↔ Siège Paris
(2, 2), -- Bob ↔ Agence Lyon
(3, 3); -- Claire ↔ Siège Marseille

-- TRAITEMENTS
INSERT INTO traitement (nom, gestionnaire, finalite_principale, id_client) VALUES
('Gestion des salariés', 'Alice Dupont', 'Administration RH', 1),
('Suivi des ventes', 'Bob Martin', 'Analyse commerciale', 1),
('Conformité RGPD', 'Claire Durand', 'Protection des données', 2);

-- LIENS TRAITEMENT ↔ ETABLISSEMENT
INSERT INTO traitement_etablissement (id_traitement, id_etablissement) VALUES
(1, 1), -- Gestion salariés ↔ Siège Paris
(2, 2), -- Suivi ventes ↔ Agence Lyon
(3, 3); -- Conformité RGPD ↔ Siège Marseille
