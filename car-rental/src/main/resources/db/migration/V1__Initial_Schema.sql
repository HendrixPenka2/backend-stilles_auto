-- Création du schéma auth (espace logique pour les tables d'authentification)
CREATE SCHEMA IF NOT EXISTS auth;

-- Table users de base (on ajoutera photo, refresh tokens, etc. plus tard)
CREATE TABLE IF NOT EXISTS auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    phone           VARCHAR(30),
    role            VARCHAR(50) NOT NULL DEFAULT 'CLIENT',
    email_verified  BOOLEAN DEFAULT FALSE,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- Index rapide pour les recherches fréquentes
CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users(email);