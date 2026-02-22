package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

import com.Team_Pk.car_rental.catalog.entity.StockMovement;

@Data
@Builder
public class StockHistoryListResponse {
    private UUID entityId;       // ID du véhicule ou accessoire
    private Integer currentStock; // Le stock actuel (pour affichage rapide)
    private List<StockMovement> movements; // La liste des changements
}
