package com.Team_Pk.car_rental.catalog.dto;

//(Réponse après modif stock)
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class StockAdjustmentResponse {
    private UUID entityId;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private UUID movementId;
}
