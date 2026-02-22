package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class VehicleCustomRepositoryImpl implements VehicleCustomRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Vehicle> findVehiclesByCriteria(VehicleSearchCriteria c) {
        StringBuilder sql = new StringBuilder("SELECT * FROM catalog.vehicles WHERE is_active = true");
        Map<String, Object> params = new HashMap<>();

        buildConditions(sql, params, c);

        // Ajout de la pagination (Tri par date de création par défaut)
        sql.append(" ORDER BY created_at DESC ");
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", c.getLimit());
        params.put("offset", (c.getPage() - 1) * c.getLimit());

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.mapProperties(Vehicle.class).all();
    }

    @Override
    public Mono<Long> countVehiclesByCriteria(VehicleSearchCriteria c) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM catalog.vehicles WHERE is_active = true");
        Map<String, Object> params = new HashMap<>();

        buildConditions(sql, params, c);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            spec = spec.bind(entry.getKey(), entry.getValue());
        }

        return spec.map((row, rowMetadata) -> row.get(0, Long.class)).one();
    }

    /**
     * C'EST ICI QUE LA MAGIE OPÈRE :
     * On ajoute dynamiquement chaque filtre UNIQUEMENT s'il a été rempli dans Swagger
     */
    private void buildConditions(StringBuilder sql, Map<String, Object> params, VehicleSearchCriteria c) {
        
        // --- 1. Filtres Textuels (Recherche partielle avec ILIKE) ---
        if (c.getBrand() != null && !c.getBrand().isBlank()) {
            sql.append(" AND brand ILIKE :brand");
            params.put("brand", "%" + c.getBrand() + "%");
        }
        if (c.getModel() != null && !c.getModel().isBlank()) {
            sql.append(" AND model ILIKE :model");
            params.put("model", "%" + c.getModel() + "%");
        }

        // --- 2. Filtres Enums (Comparaison directe avec UPPER pour éviter les problèmes de casse) ---
        if (c.getVehicleType() != null && !c.getVehicleType().isBlank()) {
            sql.append(" AND UPPER(vehicle_type::text) = UPPER(:vehicleType)");
            params.put("vehicleType", c.getVehicleType());
        }
        if (c.getFuelType() != null && !c.getFuelType().isBlank()) {
            sql.append(" AND UPPER(fuel_type::text) = UPPER(:fuelType)");
            params.put("fuelType", c.getFuelType());
        }
        if (c.getTransmission() != null && !c.getTransmission().isBlank()) {
            sql.append(" AND UPPER(transmission::text) = UPPER(:transmission)");
            params.put("transmission", c.getTransmission());
        }
        if (c.getListingMode() != null && !c.getListingMode().isBlank()) {
            sql.append(" AND listing_mode = :listingMode::catalog.listing_mode");
            params.put("listingMode", c.getListingMode());
        }
        if (c.getStatus() != null && !c.getStatus().isBlank()) {
            sql.append(" AND status = :status::catalog.vehicle_status");
            params.put("status", c.getStatus());
        }

        // --- 3. Filtres Numériques (Prix, Années, Places) ---
        if (c.getYearMin() != null) {
            sql.append(" AND year >= :yearMin");
            params.put("yearMin", c.getYearMin());
        }
        if (c.getYearMax() != null) {
            sql.append(" AND year <= :yearMax");
            params.put("yearMax", c.getYearMax());
        }
        if (c.getMinSalePrice() != null) {
            sql.append(" AND sale_price >= :minSalePrice");
            params.put("minSalePrice", c.getMinSalePrice());
        }
        if (c.getMaxSalePrice() != null) {
            sql.append(" AND sale_price <= :maxSalePrice");
            params.put("maxSalePrice", c.getMaxSalePrice());
        }
        if (c.getMinRentalPrice() != null) {
            sql.append(" AND rental_price_per_day >= :minRentalPrice");
            params.put("minRentalPrice", c.getMinRentalPrice());
        }
        if (c.getMaxRentalPrice() != null) {
            sql.append(" AND rental_price_per_day <= :maxRentalPrice");
            params.put("maxRentalPrice", c.getMaxRentalPrice());
        }
        if (c.getSeats() != null) {
            sql.append(" AND seats >= :seats"); // Cherche les véhicules avec "au moins" X places
            params.put("seats", c.getSeats());
        }

        // --- 4. Filtres Booléens ---
        if (c.getIsFeatured() != null) {
            sql.append(" AND is_featured = :isFeatured");
            params.put("isFeatured", c.getIsFeatured());
        }

        // --- 5. Recherche Globale (Mot-clé) ---
        if (c.getQ() != null && !c.getQ().isBlank()) {
            // Cherche le mot-clé dans le titre, la marque, le modèle ou la description
            sql.append(" AND (title ILIKE :q OR brand ILIKE :q OR model ILIKE :q OR description ILIKE :q)");
            params.put("q", "%" + c.getQ() + "%");
        }
    }
}