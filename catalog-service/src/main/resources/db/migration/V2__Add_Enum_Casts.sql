-- Autorise PostgreSQL à convertir automatiquement les VARCHAR envoyés par Java R2DBC vers nos ENUMs personnalisés
CREATE CAST (character varying AS catalog.vehicle_type) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.listing_mode) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.vehicle_status) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.fuel_type) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.transmission_type) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.availability_reason) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.accessory_condition) WITH INOUT AS IMPLICIT;
CREATE CAST (character varying AS catalog.stock_movement_reason) WITH INOUT AS IMPLICIT;

