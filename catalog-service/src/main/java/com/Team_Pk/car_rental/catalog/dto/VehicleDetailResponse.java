package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;

@Data
@Builder
public class VehicleDetailResponse {
    // On utilise @JsonUnwrapped ou on aplatit l'objet si on veut que les champs soient au même niveau
    // Mais pour l'instant, gardons la structure propre.
    // Pour respecter ton JSON cible, on va mapper les champs manuellement dans le Service.
    
    private Vehicle vehicle; 
    private List<VehicleImage> images;
    
    // Nouveaux champs pour les notes
    private Double averageRating;
    private Integer reviewCount;
}