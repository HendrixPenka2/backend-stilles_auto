CREATE SCHEMA IF NOT EXISTS catalog;

-- Extensions indispensables
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "unaccent";
CREATE EXTENSION IF NOT EXISTS btree_gist; -- OBLIGATOIRE POUR LE EXCLUDE GIST

-- ==========================================
-- 1. CRÉATION DES TYPES (ENUMS)
-- ==========================================
CREATE TYPE catalog.vehicle_type AS ENUM ('SEDAN', 'SUV', 'PICKUP', 'VAN', 'TRUCK', 'MOTO', 'COUPE', 'MINIBUS');
CREATE TYPE catalog.listing_mode AS ENUM ('SALE_ONLY', 'RENTAL_ONLY', 'BOTH');
CREATE TYPE catalog.vehicle_status AS ENUM ('AVAILABLE', 'RESERVED', 'RENTED', 'SOLD', 'MAINTENANCE');
CREATE TYPE catalog.fuel_type AS ENUM ('ESSENCE', 'DIESEL', 'HYBRID', 'ELECTRIC', 'LPG');
CREATE TYPE catalog.transmission_type AS ENUM ('MANUAL', 'AUTOMATIC', 'SEMI_AUTOMATIC');
CREATE TYPE catalog.availability_reason AS ENUM ('RENTED', 'MAINTENANCE', 'RESERVED', 'OTHER');
CREATE TYPE catalog.accessory_condition AS ENUM ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR');
CREATE TYPE catalog.stock_movement_reason AS ENUM ('INITIAL_STOCK', 'PURCHASE_IN', 'SALE_OUT', 'RENTAL_OUT', 'RENTAL_RETURN', 'ADJUSTMENT', 'LOSS', 'RETURN_IN');

-- ==========================================
-- 2. TABLES DES VÉHICULES
-- ==========================================
CREATE TABLE catalog.vehicles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title               VARCHAR(255) NOT NULL,
    brand               VARCHAR(100) NOT NULL,
    model               VARCHAR(100) NOT NULL,
    year                SMALLINT NOT NULL CHECK (year BETWEEN 1900 AND 2100),
    color               VARCHAR(50),
    vin                 VARCHAR(17) UNIQUE,
    vehicle_type        catalog.vehicle_type NOT NULL,
    fuel_type           catalog.fuel_type NOT NULL,
    transmission        catalog.transmission_type NOT NULL,
    mileage             INTEGER NOT NULL DEFAULT 0 CHECK (mileage >= 0),
    engine_capacity     DECIMAL(4, 1),
    horsepower          SMALLINT,
    doors               SMALLINT CHECK (doors BETWEEN 1 AND 10),
    seats               SMALLINT CHECK (seats BETWEEN 1 AND 50),
    listing_mode        catalog.listing_mode NOT NULL,
    sale_price          DECIMAL(15, 2),
    rental_price_per_day DECIMAL(10, 2),
    stock_quantity      SMALLINT NOT NULL DEFAULT 1 CHECK (stock_quantity >= 0),
    status              catalog.vehicle_status NOT NULL DEFAULT 'AVAILABLE',
    description         TEXT,
    features            JSONB DEFAULT '[]',
    is_featured         BOOLEAN NOT NULL DEFAULT FALSE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sale_price CHECK (listing_mode = 'RENTAL_ONLY' OR sale_price IS NOT NULL),
    CONSTRAINT chk_rental_price CHECK (listing_mode = 'SALE_ONLY' OR rental_price_per_day IS NOT NULL)
);

CREATE INDEX idx_vehicles_brand           ON catalog.vehicles (brand);
CREATE INDEX idx_vehicles_type            ON catalog.vehicles (vehicle_type);
CREATE INDEX idx_vehicles_listing_mode    ON catalog.vehicles (listing_mode);
CREATE INDEX idx_vehicles_status          ON catalog.vehicles (status);
CREATE INDEX idx_vehicles_sale_price      ON catalog.vehicles (sale_price) WHERE sale_price IS NOT NULL;
CREATE INDEX idx_vehicles_rental_price    ON catalog.vehicles (rental_price_per_day) WHERE rental_price_per_day IS NOT NULL;
CREATE INDEX idx_vehicles_year            ON catalog.vehicles (year DESC);
CREATE INDEX idx_vehicles_is_active       ON catalog.vehicles (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_vehicles_fts ON catalog.vehicles USING GIN (to_tsvector('french', (title || ' ' || brand || ' ' || model)));

CREATE TABLE catalog.vehicle_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id      UUID NOT NULL REFERENCES catalog.vehicles(id) ON DELETE CASCADE,
    url             TEXT NOT NULL,
    thumbnail_url   TEXT,
    alt_text        VARCHAR(255),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   SMALLINT NOT NULL DEFAULT 0,
    file_size_bytes INTEGER,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicle_images_vehicle_id ON catalog.vehicle_images (vehicle_id);
CREATE UNIQUE INDEX idx_vehicle_images_primary ON catalog.vehicle_images (vehicle_id) WHERE is_primary = TRUE;

CREATE TABLE catalog.rental_availability (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id      UUID NOT NULL REFERENCES catalog.vehicles(id) ON DELETE CASCADE,
    date_range      DATERANGE NOT NULL,
    reason          catalog.availability_reason NOT NULL DEFAULT 'RENTED',
    reference_id    UUID,
    notes           TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rental_availability_vehicle_range ON catalog.rental_availability USING GIST (vehicle_id, date_range);
ALTER TABLE catalog.rental_availability ADD CONSTRAINT no_overlapping_availability EXCLUDE USING GIST (vehicle_id WITH =, date_range WITH &&) WHERE (expires_at IS NULL);

-- ==========================================
-- 3. TABLES DES ACCESSOIRES
-- ==========================================
CREATE TABLE catalog.accessories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    brand           VARCHAR(100),
    sku             VARCHAR(100) UNIQUE,
    category        VARCHAR(100) NOT NULL,
    sub_category    VARCHAR(100),
    compatible_brands TEXT[],
    compatible_models TEXT[],
    price           DECIMAL(12, 2) NOT NULL CHECK (price > 0),
    compare_price   DECIMAL(12, 2),
    stock_quantity  INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    low_stock_alert INTEGER NOT NULL DEFAULT 5,
    condition       catalog.accessory_condition NOT NULL DEFAULT 'NEW',
    weight_kg       DECIMAL(6, 2),
    dimensions      JSONB,
    description     TEXT,
    specifications  JSONB DEFAULT '{}',
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accessories_category     ON catalog.accessories (category);
CREATE INDEX idx_accessories_brand        ON catalog.accessories (brand);
CREATE INDEX idx_accessories_price        ON catalog.accessories (price);
CREATE INDEX idx_accessories_stock        ON catalog.accessories (stock_quantity);
CREATE INDEX idx_accessories_is_active    ON catalog.accessories (is_active) WHERE is_active = TRUE;
CREATE INDEX idx_accessories_fts ON catalog.accessories USING GIN (to_tsvector('french', (name || ' ' || COALESCE(brand, '') || ' ' || category)));

CREATE TABLE catalog.accessory_images (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    accessory_id    UUID NOT NULL REFERENCES catalog.accessories(id) ON DELETE CASCADE,
    url             TEXT NOT NULL,
    thumbnail_url   TEXT,
    alt_text        VARCHAR(255),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    display_order   SMALLINT NOT NULL DEFAULT 0,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_accessory_images_accessory_id ON catalog.accessory_images (accessory_id);
CREATE UNIQUE INDEX idx_accessory_images_primary ON catalog.accessory_images (accessory_id) WHERE is_primary = TRUE;

-- ==========================================
-- 4. STOCK MOVEMENTS (Audit)
-- ==========================================
CREATE TABLE catalog.stock_movements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(20) NOT NULL CHECK (entity_type IN ('VEHICLE', 'ACCESSORY')),
    entity_id       UUID NOT NULL,
    quantity_delta  INTEGER NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_after  INTEGER NOT NULL,
    reason          catalog.stock_movement_reason NOT NULL,
    reference_id    UUID,
    notes           TEXT,
    performed_by    UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_entity ON catalog.stock_movements (entity_type, entity_id, created_at DESC);

-- ==========================================
-- 5. TRIGGERS ET VUES
-- ==========================================
CREATE OR REPLACE FUNCTION catalog.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vehicles_updated_at BEFORE UPDATE ON catalog.vehicles FOR EACH ROW EXECUTE FUNCTION catalog.set_updated_at();
CREATE TRIGGER trg_accessories_updated_at BEFORE UPDATE ON catalog.accessories FOR EACH ROW EXECUTE FUNCTION catalog.set_updated_at();

CREATE VIEW catalog.vehicles_public AS
SELECT v.id, v.title, v.brand, v.model, v.year, v.vehicle_type, v.listing_mode, v.fuel_type, v.transmission, v.mileage, v.seats, v.color, v.sale_price, v.rental_price_per_day, v.status, v.features, v.is_featured, vi.url AS primary_image_url, vi.thumbnail_url AS primary_thumbnail_url
FROM catalog.vehicles v
LEFT JOIN catalog.vehicle_images vi ON vi.vehicle_id = v.id AND vi.is_primary = TRUE
WHERE v.is_active = TRUE;

CREATE VIEW catalog.accessories_public AS
SELECT a.id, a.name, a.brand, a.category, a.sub_category, a.price, a.compare_price, a.stock_quantity, a.condition, a.is_featured, ai.url AS primary_image_url, ai.thumbnail_url AS primary_thumbnail_url
FROM catalog.accessories a
LEFT JOIN catalog.accessory_images ai ON ai.accessory_id = a.id AND ai.is_primary = TRUE
WHERE a.is_active = TRUE;