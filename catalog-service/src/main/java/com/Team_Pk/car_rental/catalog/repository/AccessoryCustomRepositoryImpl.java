package com.Team_Pk.car_rental.catalog.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;

import com.Team_Pk.car_rental.catalog.dto.AccessoryFilterOptions;
import com.Team_Pk.car_rental.catalog.dto.AccessorySearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Accessory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AccessoryCustomRepositoryImpl implements AccessoryCustomRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Accessory> findAccessoriesByCriteria(AccessorySearchCriteria c) {
        StringBuilder sql = new StringBuilder("SELECT * FROM catalog.accessories WHERE is_active = true");
        Map<String, Object> params = new HashMap<>();
        buildConditions(sql, params, c);

        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");
        params.put("limit", c.getLimit());
        params.put("offset", (c.getPage() - 1) * c.getLimit());

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) spec = spec.bind(entry.getKey(), entry.getValue());
        return spec.mapProperties(Accessory.class).all();
    }

    @Override
    public Mono<Long> countAccessoriesByCriteria(AccessorySearchCriteria c) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM catalog.accessories WHERE is_active = true");
        Map<String, Object> params = new HashMap<>();
        buildConditions(sql, params, c);

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());
        for (Map.Entry<String, Object> entry : params.entrySet()) spec = spec.bind(entry.getKey(), entry.getValue());
        return spec.map((row, meta) -> row.get(0, Long.class)).one();
    }

    private void buildConditions(StringBuilder sql, Map<String, Object> params, AccessorySearchCriteria c) {
        if (c.getQ() != null && !c.getQ().isBlank()) {
            sql.append(" AND (name ILIKE :q OR brand ILIKE :q OR category ILIKE :q)");
            params.put("q", "%" + c.getQ() + "%");
        }
        if (c.getCategory() != null) {
            sql.append(" AND category = :category");
            params.put("category", c.getCategory());
        }
        if (c.getSubCategory() != null) {
            sql.append(" AND sub_category = :subCategory");
            params.put("subCategory", c.getSubCategory());
        }
        if (c.getBrand() != null) {
            sql.append(" AND brand = :brand");
            params.put("brand", c.getBrand());
        }
        // Gestion des tableaux Postgres (compatible_brands est TEXT[])
        if (c.getCompatibleBrand() != null) {
            sql.append(" AND :compBrand = ANY(compatible_brands)");
            params.put("compBrand", c.getCompatibleBrand());
        }
        if (c.getInStock() != null && c.getInStock()) sql.append(" AND stock_quantity > 0");
        if (c.getIsFeatured() != null) {
            sql.append(" AND is_featured = :isFeatured");
            params.put("isFeatured", c.getIsFeatured());
        }
    }

    @Override
    public Mono<AccessoryFilterOptions> getFilterOptions() {
        // Pour faire simple, on récupère juste le prix min/max ici
        return databaseClient.sql("SELECT MIN(price) as min, MAX(price) as max FROM catalog.accessories WHERE is_active = true")
            .map((row, meta) -> AccessoryFilterOptions.builder()
                .price(AccessoryFilterOptions.MinMax.builder()
                    .min(row.get("min", Number.class))
                    .max(row.get("max", Number.class)).build())
                .brands(List.of("Bosch", "Valeo")) // À dynamiser avec DISTINCT plus tard si besoin
                .categories(List.of()) 
                .build()
            ).one();
    }
}
