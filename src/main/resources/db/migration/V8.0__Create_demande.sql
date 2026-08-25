CREATE TABLE demande
(
    id UUID NOT NULL PRIMARY KEY,

    type_demande VARCHAR(255) NOT NULL,

    description_synthetique TEXT,

    origine VARCHAR(255),

    date_reception DATE,

    services_concernes TEXT,

    detail_traitement TEXT,

    services_impliques TEXT,

    reponse TEXT,

    alerte_rt VARCHAR(255),

    statut VARCHAR(50) NOT NULL,

    id_client UUID NOT NULL,

    CONSTRAINT fk_demande_client
        FOREIGN KEY (id_client)
            REFERENCES client (id)
            ON DELETE CASCADE
);