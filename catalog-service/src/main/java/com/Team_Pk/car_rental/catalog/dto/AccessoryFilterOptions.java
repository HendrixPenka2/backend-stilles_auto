package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

// DTO pour les options de filtrage des accessoires (pour construire les filtres dynamiques côté front)
@Data
@Builder
public class AccessoryFilterOptions {
    private List<String> categories;
    private List<String> brands;
    private List<String> conditions;
    private MinMax price;
    private List<String> compatibleBrands;

    @Data
    @Builder
    public static class MinMax {
        private Number min;
        private Number max;
    }
}
