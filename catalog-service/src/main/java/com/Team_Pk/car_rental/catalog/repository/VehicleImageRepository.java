package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VehicleImageRepository extends ReactiveCrudRepository<VehicleImage, UUID> {
    
    // Récupérer toutes les images d'un véhicule triées par ordre d'affichage
    Flux<VehicleImage> findByVehicleIdOrderByDisplayOrderAsc(UUID vehicleId);
    
    // Compter le nombre d'images d'un véhicule (pour savoir si on doit mettre isPrimary = true)
    Mono<Long> countByVehicleId(UUID vehicleId);
    
    // Trouver l'image principale d'un véhicule
    Mono<VehicleImage> findByVehicleIdAndIsPrimaryTrue(UUID vehicleId);
}