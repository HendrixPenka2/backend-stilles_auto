package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VehicleCustomRepository {
    // Récupère la liste filtrée et paginée
    Flux<Vehicle> findVehiclesByCriteria(VehicleSearchCriteria criteria);
    
    // Récupère le nombre total de résultats (indispensable pour la pagination front)
    Mono<Long> countVehiclesByCriteria(VehicleSearchCriteria criteria);
}