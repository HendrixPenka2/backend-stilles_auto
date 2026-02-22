package com.Team_Pk.car_rental.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;

import com.Team_Pk.car_rental.catalog.dto.CatalogStatsResponse;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatalogStatsService {

    private final DatabaseClient databaseClient;

    public Mono<CatalogStatsResponse> getDashboardStats() {
        return Mono.zip(
                getVehicleStats(),
                getAccessoryStats(),
                getRecentMovements()
        ).map(tuple -> CatalogStatsResponse.builder()
                .vehicles(tuple.getT1())
                .accessories(tuple.getT2())
                .recentMovements(tuple.getT3())
                .build());
    }

    // 1. STATISTIQUES VÉHICULES
    private Mono<CatalogStatsResponse.VehicleStats> getVehicleStats() {
        Mono<Long> total = databaseClient.sql("SELECT COUNT(*) FROM catalog.vehicles WHERE is_active = true").mapValue(Long.class).one();
        
        Mono<Map<String, Long>> byStatus = databaseClient.sql("SELECT status, COUNT(*) FROM catalog.vehicles WHERE is_active = true GROUP BY status")
                .map((row, meta) -> Map.entry(row.get("status", String.class), row.get("count", Long.class)))
                .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);

        Mono<Map<String, Long>> byListingMode = databaseClient.sql("SELECT listing_mode, COUNT(*) FROM catalog.vehicles WHERE is_active = true GROUP BY listing_mode")
                .map((row, meta) -> Map.entry(row.get("listing_mode", String.class), row.get("count", Long.class)))
                .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);

        return Mono.zip(total, byStatus, byListingMode).map(t -> 
                CatalogStatsResponse.VehicleStats.builder()
                        .total(t.getT1())
                        .byStatus(t.getT2())
                        .byListingMode(t.getT3())
                        .build());
    }

    // 2. STATISTIQUES ACCESSOIRES
    private Mono<CatalogStatsResponse.AccessoryStats> getAccessoryStats() {
        Mono<Long> total = databaseClient.sql("SELECT COUNT(*) FROM catalog.accessories WHERE is_active = true").mapValue(Long.class).one();
        
        Mono<BigDecimal> totalValue = databaseClient.sql("SELECT SUM(price * stock_quantity) FROM catalog.accessories WHERE is_active = true AND stock_quantity > 0")
                .mapValue(BigDecimal.class).first().defaultIfEmpty(BigDecimal.ZERO); // Evite l'erreur si la base est vide
                
        Mono<Long> lowStock = databaseClient.sql("SELECT COUNT(*) FROM catalog.accessories WHERE is_active = true AND stock_quantity <= low_stock_alert AND stock_quantity > 0").mapValue(Long.class).one();
        
        Mono<Long> outOfStock = databaseClient.sql("SELECT COUNT(*) FROM catalog.accessories WHERE is_active = true AND stock_quantity = 0").mapValue(Long.class).one();

        Mono<Map<String, Long>> byCategory = databaseClient.sql("SELECT category, COUNT(*) FROM catalog.accessories WHERE is_active = true GROUP BY category")
                .map((row, meta) -> Map.entry(row.get("category", String.class), row.get("count", Long.class)))
                .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);

        return Mono.zip(total, totalValue, lowStock, outOfStock, byCategory).map(t -> 
                CatalogStatsResponse.AccessoryStats.builder()
                        .total(t.getT1())
                        .totalStockValue(t.getT2())
                        .lowStockCount(t.getT3())
                        .outOfStockCount(t.getT4())
                        .byCategory(t.getT5())
                        .build());
    }

    // 3. MOUVEMENTS RÉCENTS (Avec Jointures SQL)
    private Mono<java.util.List<CatalogStatsResponse.RecentMovement>> getRecentMovements() {
        String sql = "SELECT sm.entity_type, " +
                     "COALESCE(v.title, a.name) as entity_name, " + // COALESCE prend le titre du véhicule, ou s'il est null, le nom de l'accessoire
                     "sm.quantity_delta, sm.reason, sm.created_at " +
                     "FROM catalog.stock_movements sm " +
                     "LEFT JOIN catalog.vehicles v ON sm.entity_type = 'VEHICLE' AND sm.entity_id = v.id " +
                     "LEFT JOIN catalog.accessories a ON sm.entity_type = 'ACCESSORY' AND sm.entity_id = a.id " +
                     "ORDER BY sm.created_at DESC LIMIT 5";

        return databaseClient.sql(sql)
                .map((row, meta) -> CatalogStatsResponse.RecentMovement.builder()
                        .entityType(row.get("entity_type", String.class))
                        .entityName(row.get("entity_name", String.class))
                        .quantityDelta(row.get("quantity_delta", Integer.class))
                        .reason(row.get("reason", String.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .build())
                .all()
                .collectList();
    }
}