-- ============================================
-- Tables principales
-- ============================================

-- Table CLIENT
CREATE TABLE client
(
    id     SERIAL PRIMARY KEY,
    nom    VARCHAR(255) NOT NULL,
    statut VARCHAR(100)
);

-- Table PROFIL
CREATE TABLE profil
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Table UTILISATEUR
CREATE TABLE utilisateur
(
    id        SERIAL PRIMARY KEY,
    prenom    VARCHAR(100) NOT NULL,
    nom       VARCHAR(100) NOT NULL,
    email     VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    fonction  VARCHAR(100),
    id_profil INT          NOT NULL,
    id_client INT          NOT NULL,
    CONSTRAINT fk_utilisateur_profil FOREIGN KEY (id_profil) REFERENCES profil (id) ON DELETE RESTRICT,
    CONSTRAINT fk_utilisateur_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE
);

-- Table ETABLISSEMENT
CREATE TABLE etablissement
(
    id        SERIAL PRIMARY KEY,
    nom       VARCHAR(255) NOT NULL,
    id_client INT          NOT NULL,
    CONSTRAINT fk_etablissement_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE
);

-- ============================================
-- Tables de liaison (N-N)
-- ============================================

-- Table UTILISATEUR_ETABLISSEMENT
CREATE TABLE utilisateur_etablissement
(
    id               SERIAL PRIMARY KEY,
    id_utilisateur   INT NOT NULL,
    id_etablissement INT NOT NULL,
    CONSTRAINT fk_useretab_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES utilisateur (id) ON DELETE CASCADE,
    CONSTRAINT fk_useretab_etablissement FOREIGN KEY (id_etablissement) REFERENCES etablissement (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_etab UNIQUE (id_utilisateur, id_etablissement)
);

-- Table TRAITEMENT
CREATE TABLE traitement
(
    id                  SERIAL PRIMARY KEY,
    nom                 VARCHAR(255) NOT NULL,
    gestionnaire        VARCHAR(255),
    finalite_principale VARCHAR(255),
    id_client           INT          NOT NULL,
    CONSTRAINT fk_traitement_client FOREIGN KEY (id_client) REFERENCES client (id) ON DELETE CASCADE
);

-- Table TRAITEMENT_ETABLISSEMENT
CREATE TABLE traitement_etablissement
(
    id               SERIAL PRIMARY KEY,
    id_traitement    INT NOT NULL,
    id_etablissement INT NOT NULL,
    CONSTRAINT fk_traitetab_traitement FOREIGN KEY (id_traitement) REFERENCES traitement (id) ON DELETE CASCADE,
    CONSTRAINT fk_traitetab_etablissement FOREIGN KEY (id_etablissement) REFERENCES etablissement (id) ON DELETE CASCADE,
    CONSTRAINT uq_traitement_etab UNIQUE (id_traitement, id_etablissement)
);
