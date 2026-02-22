package com.Team_Pk.car_rental.catalog.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.Team_Pk.car_rental.catalog.entity.enums.StockMovementReason;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_movements", schema = "catalog")
public class StockMovement {

    @Id
    private UUID id;

    private String entityType; // "VEHICLE" ou "ACCESSORY"
    private UUID entityId;
    
    private Integer quantityDelta;
    private Integer quantityBefore;
    private Integer quantityAfter;
    
    private StockMovementReason reason;
    private UUID referenceId; // ID de la commande ou de la location
    private String notes;
    
    private UUID performedBy; // Qui a fait ça ? (L'Admin ou le Système)

    @CreatedDate
    private Instant createdAt;
}
