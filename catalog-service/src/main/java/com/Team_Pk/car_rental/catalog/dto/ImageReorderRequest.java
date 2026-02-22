package com.Team_Pk.car_rental.catalog.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ImageReorderRequest {
    
    private List<ImageOrder> order;

    @Data
    public static class ImageOrder {
        private UUID imageId;
        private Integer displayOrder; // 0, 1, 2, 3...
    }
}

