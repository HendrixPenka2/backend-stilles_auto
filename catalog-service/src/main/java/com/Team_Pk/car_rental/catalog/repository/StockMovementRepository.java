package com.Team_Pk.car_rental.catalog.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.catalog.entity.StockMovement;

import reactor.core.publisher.Flux;

public interface StockMovementRepository extends ReactiveCrudRepository<StockMovement, UUID> {
    // Pour afficher l'historique d'un véhicule spécifique
    Flux<StockMovement> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, UUID entityId);
}
