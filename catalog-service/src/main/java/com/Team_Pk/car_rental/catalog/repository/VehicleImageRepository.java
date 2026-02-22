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

    // 1. Remettre toutes les images du véhicule à is_primary = false
    @org.springframework.data.r2dbc.repository.Modifying
    @org.springframework.data.r2dbc.repository.Query("UPDATE catalog.vehicle_images SET is_primary = false WHERE vehicle_id = :vehicleId")
    Mono<Integer> resetPrimaryStatus(UUID vehicleId);

    // 2. Mettre à jour l'ordre et le statut d'une image spécifique
    @org.springframework.data.r2dbc.repository.Modifying
    @org.springframework.data.r2dbc.repository.Query("UPDATE catalog.vehicle_images SET display_order = :displayOrder, is_primary = :isPrimary WHERE id = :imageId AND vehicle_id = :vehicleId")
    Mono<Integer> updateImageOrderAndPrimary(UUID imageId, UUID vehicleId, Integer displayOrder, Boolean isPrimary);
}