package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.dto.FilterOptionsResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VehicleCustomRepository {
    // Récupère la liste filtrée et paginée (uniquement véhicules actifs)
    Flux<Vehicle> findVehiclesByCriteria(VehicleSearchCriteria criteria);
    
    // Récupère le nombre total de résultats (indispensable pour la pagination front)
    Mono<Long> countVehiclesByCriteria(VehicleSearchCriteria criteria);

    // Récupère les options de filtrage (pour construire les filtres dynamiques côté front)
    Mono<FilterOptionsResponse> getFilterOptions();
    
    // ==========================================
    // MÉTHODES ADMIN (incluent les véhicules inactifs)
    // ==========================================
    
    // Récupère la liste filtrée et paginée POUR ADMIN (inclut les véhicules inactifs)
    Flux<Vehicle> findAllVehiclesByCriteria(VehicleSearchCriteria criteria);
    
    // Récupère le nombre total POUR ADMIN (inclut les véhicules inactifs)
    Mono<Long> countAllVehiclesByCriteria(VehicleSearchCriteria criteria);
}