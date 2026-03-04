-- ============================================================
-- SCHÉMA : community
-- ============================================================
CREATE SCHEMA IF NOT EXISTS community;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUMS
-- ============================================================
CREATE TYPE community.review_entity_type   AS ENUM ('VEHICLE', 'ACCESSORY');
CREATE TYPE community.review_status        AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE community.wishlist_entity_type AS ENUM ('VEHICLE', 'ACCESSORY');

-- ============================================================
-- TABLE : reviews
-- ============================================================
CREATE TABLE community.reviews (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    entity_type  community.review_entity_type NOT NULL,
    entity_id    UUID NOT NULL,
    rating       SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title        VARCHAR(150),
    comment      TEXT NOT NULL,
    status       community.review_status NOT NULL DEFAULT 'PENDING',
    admin_note   TEXT,
    moderated_by UUID,
    moderated_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX idx_reviews_entity  ON community.reviews (entity_type, entity_id, status);
CREATE INDEX idx_reviews_user_id ON community.reviews (user_id);
CREATE INDEX idx_reviews_status  ON community.reviews (status, created_at DESC);

-- ============================================================
-- TABLE : wishlist_items
-- ============================================================
CREATE TABLE community.wishlist_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    entity_type community.wishlist_entity_type NOT NULL,
    entity_id   UUID NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (user_id, entity_type, entity_id)
);

CREATE INDEX idx_wishlist_user_id ON community.wishlist_items (user_id);
CREATE INDEX idx_wishlist_entity  ON community.wishlist_items (entity_type, entity_id);

-- ============================================================
-- VUE MATÉRIALISÉE : entity_ratings
-- ============================================================
CREATE MATERIALIZED VIEW community.entity_ratings AS
SELECT
    entity_type,
    entity_id,
    ROUND(AVG(rating), 2) AS average_rating,
    COUNT(*)              AS review_count
FROM community.reviews
WHERE status = 'APPROVED'
GROUP BY entity_type, entity_id;

CREATE UNIQUE INDEX ON community.entity_ratings (entity_type, entity_id);

-- ============================================================
-- TRIGGERS
-- ============================================================
CREATE OR REPLACE FUNCTION community.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON community.reviews
    FOR EACH ROW EXECUTE FUNCTION community.set_updated_at();

-- ============================================================
-- FIX R2DBC ENUMS (Très important !)
-- ============================================================
CREATE CAST (character varying AS community.review_entity_type) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS community.review_status) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS community.wishlist_entity_type) WITH INOUT AS IMPLICIT;