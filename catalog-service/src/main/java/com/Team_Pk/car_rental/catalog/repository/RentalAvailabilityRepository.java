package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.dto.AvailabilityResponse;
import com.Team_Pk.car_rental.catalog.entity.Vehicle; // Juste pour le type générique si besoin
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface RentalAvailabilityRepository extends ReactiveCrudRepository<Vehicle, UUID> {

    // 1. Lire les dates (On utilise lower() et upper() de Postgres pour extraire les dates du DATERANGE)
    @Query("SELECT id, lower(date_range) as start_date, upper(date_range) as end_date, reason " +
           "FROM catalog.rental_availability WHERE vehicle_id = :vehicleId " +
           "AND upper(date_range) >= CURRENT_DATE") // On ne prend que les dates futures
    Flux<AvailabilityResponse> getFutureAvailability(UUID vehicleId);

    // 2. Insérer une date (On utilise daterange() de Postgres pour créer la plage)
    @Query("INSERT INTO catalog.rental_availability (vehicle_id, date_range, reason, notes) " +
           "VALUES (:vehicleId, daterange(:startDate::date, :endDate::date, '[]'), :reason::catalog.availability_reason, :notes) " +
           "RETURNING id")
    Mono<UUID> blockPeriod(UUID vehicleId, LocalDate startDate, LocalDate endDate, String reason, String notes);
    
    // 3. Supprimer un blocage
    @Query("DELETE FROM catalog.rental_availability WHERE id = :blockId AND vehicle_id = :vehicleId")
    Mono<Void> deleteBlock(UUID blockId, UUID vehicleId);
}