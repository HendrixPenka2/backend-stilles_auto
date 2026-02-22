package com.Team_Pk.car_rental.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CatalogStatsResponse {

    private VehicleStats vehicles;
    private AccessoryStats accessories;
    
    @JsonProperty("recent_movements")
    private List<RecentMovement> recentMovements;

    @Data
    @Builder
    public static class VehicleStats {
        private long total;
        
        @JsonProperty("by_status")
        private Map<String, Long> byStatus;
        
        @JsonProperty("by_listing_mode")
        private Map<String, Long> byListingMode;
    }

    @Data
    @Builder
    public static class AccessoryStats {
        private long total;
        
        @JsonProperty("total_stock_value")
        private BigDecimal totalStockValue;
        
        @JsonProperty("low_stock_count")
        private long lowStockCount;
        
        @JsonProperty("out_of_stock_count")
        private long outOfStockCount;
        
        @JsonProperty("by_category")
        private Map<String, Long> byCategory;
    }

    @Data
    @Builder
    public static class RecentMovement {
        @JsonProperty("entity_type")
        private String entityType;
        
        @JsonProperty("entity_name")
        private String entityName;
        
        @JsonProperty("quantity_delta")
        private Integer quantityDelta;
        
        private String reason;
        
        @JsonProperty("created_at")
        private Instant createdAt;
    }
}