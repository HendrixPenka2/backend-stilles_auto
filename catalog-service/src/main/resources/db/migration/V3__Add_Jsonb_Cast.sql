-- Autorise PostgreSQL à convertir automatiquement les VARCHAR (Strings Java) en JSONB
CREATE CAST (character varying AS jsonb) WITH INOUT AS IMPLICIT;