package com.Team_Pk.car_rental.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO pour recevoir les événements RabbitMQ de community-service
 * 
 * Structure identique à celle envoyée par ReviewService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEvent {
    private UUID reviewId;
    private UUID userId;
    private String entityType;  // "VEHICLE" ou "ACCESSORY"
    private UUID entityId;      // ID du véhicule ou accessoire
    private Integer rating;     // Note 1-5
    private String status;      // "PENDING", "APPROVED", "REJECTED"
}
