package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

// L'interface hérite de ReactiveCrudRepository ET de ton CustomRepository
public interface VehicleRepository extends ReactiveCrudRepository<Vehicle, UUID>, VehicleCustomRepository {
    
    // Requête classique pour vérifier l'unicité du numéro de châssis par exemple
    Mono<Boolean> existsByVin(String vin);
    
}