package com.Team_Pk.car_rental.catalog.dto;

// (Réponse complète avec images)

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.Team_Pk.car_rental.catalog.entity.Accessory;
import com.Team_Pk.car_rental.catalog.entity.AccessoryImage;

@Data
@Builder
public class AccessoryDetailResponse {
    private Accessory accessory;
    private List<AccessoryImage> images;
    private Double averageRating; // Placeholder pour le futur service Community
    private Integer reviewCount;  // Placeholder
}