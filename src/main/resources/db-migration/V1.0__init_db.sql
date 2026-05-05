create EXTENSION IF NOT EXISTS "uuid-ossp";
create EXTENSION IF NOT EXISTS pgcrypto;

create TABLE TEST (
    identifiant UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    label varchar unique not null
);
