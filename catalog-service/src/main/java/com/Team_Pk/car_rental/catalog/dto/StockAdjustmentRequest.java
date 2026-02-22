package com.Team_Pk.car_rental.catalog.dto;

import com.Team_Pk.car_rental.catalog.entity.enums.StockMovementReason;


import lombok.Data;
//(Pour modifier le stock)
@Data
public class StockAdjustmentRequest {
    private Integer quantityDelta; // ex: +50 ou -5
    private StockMovementReason reason; // PURCHASE_IN, LOSS...
    private String notes;
}