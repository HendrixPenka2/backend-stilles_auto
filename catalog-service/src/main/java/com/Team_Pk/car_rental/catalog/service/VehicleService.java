package com.Team_Pk.car_rental.catalog.service;


import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleDetailResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleStatus;
import com.Team_Pk.car_rental.catalog.repository.VehicleImageRepository;
import com.Team_Pk.car_rental.catalog.repository.VehicleRepository;
import com.Team_Pk.car_rental.catalog.dto.VehicleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository imageRepository;
    

    // ==========================================
    // MÉTHODES PUBLIQUES
    // ==========================================

    public Mono<PaginatedResponse<Vehicle>> searchVehicles(VehicleSearchCriteria criteria) {
        // 1. On lance les deux requêtes à la BDD en parallèle
        Mono<List<Vehicle>> dataMono = vehicleRepository.findVehiclesByCriteria(criteria).collectList();
        Mono<Long> countMono = vehicleRepository.countVehiclesByCriteria(criteria);

        // 2. On attend les deux résultats et on construit la réponse
        return Mono.zip(dataMono, countMono).map(tuple -> {
            List<Vehicle> data = tuple.getT1();
            long total = tuple.getT2();
            int totalPages = (int) Math.ceil((double) total / criteria.getLimit());

            PaginatedResponse.Meta meta = PaginatedResponse.Meta.builder()
                    .total(total)
                    .page(criteria.getPage())
                    .limit(criteria.getLimit())
                    .totalPages(totalPages)
                    .hasNext(criteria.getPage() < totalPages)
                    .hasPrev(criteria.getPage() > 1)
                    .build();

            return PaginatedResponse.<Vehicle>builder()
                    .data(data)
                    .meta(meta)
                    .build();
        });
    }

    public Mono<VehicleDetailResponse> getVehicleById(UUID id) {
        // 1. Cherche le véhicule
        Mono<Vehicle> vehicleMono = vehicleRepository.findById(id)
                .filter(Vehicle::getIsActive)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));

        // 2. Cherche les images
        Mono<List<VehicleImage>> imagesMono = imageRepository
                .findByVehicleIdOrderByDisplayOrderAsc(id)
                .collectList(); // Transforme le Flux en une seule Liste

        // 3. Combine les deux dans notre DTO
        return Mono.zip(vehicleMono, imagesMono).map(tuple -> 
                VehicleDetailResponse.builder()
                        .vehicle(tuple.getT1())
                        .images(tuple.getT2())
                        .build()
        );
    }

    // ==========================================
    // MÉTHODES ADMIN
    // ==========================================

    public Mono<Vehicle> createVehicle(VehicleRequest req, UUID adminId) {
        // Optionnel : vérifier si le VIN existe déjà
        return vehicleRepository.existsByVin(req.getVin())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new RuntimeException("Ce VIN existe déjà."));

                    Vehicle vehicle = Vehicle.builder()
                            .title(req.getTitle())
                            .brand(req.getBrand())
                            .model(req.getModel())
                            .year(req.getYear())
                            .color(req.getColor())
                            .vin(req.getVin())
                            .vehicleType(req.getVehicleType())
                            .fuelType(req.getFuelType())
                            .transmission(req.getTransmission())
                            .mileage(req.getMileage())
                            .engineCapacity(req.getEngineCapacity())
                            .horsepower(req.getHorsepower())
                            .doors(req.getDoors())
                            .seats(req.getSeats())
                            .listingMode(req.getListingMode())
                            .salePrice(req.getSalePrice())
                            .rentalPricePerDay(req.getRentalPricePerDay())
                            .stockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 1)
                            .status(req.getStatus() != null ? req.getStatus() : VehicleStatus.AVAILABLE)
                            .description(req.getDescription())
                            .features(req.getFeatures())
                            .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                            .isActive(true)
                            .createdBy(adminId) // L'ID du token JWT de l'admin
                            .build();

                    return vehicleRepository.save(vehicle);
                });
    }

    public Mono<Vehicle> updateVehicle(UUID id, VehicleRequest req) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    // On met à jour TOUS les champs fournis (Patch partiel)
                    if (req.getTitle() != null) vehicle.setTitle(req.getTitle());
                    if (req.getBrand() != null) vehicle.setBrand(req.getBrand());
                    if (req.getModel() != null) vehicle.setModel(req.getModel());
                    if (req.getYear() != null) vehicle.setYear(req.getYear());
                    if (req.getColor() != null) vehicle.setColor(req.getColor());
                    if (req.getVin() != null) vehicle.setVin(req.getVin());
                    if (req.getVehicleType() != null) vehicle.setVehicleType(req.getVehicleType());
                    if (req.getFuelType() != null) vehicle.setFuelType(req.getFuelType());
                    if (req.getTransmission() != null) vehicle.setTransmission(req.getTransmission());
                    if (req.getMileage() != null) vehicle.setMileage(req.getMileage());
                    if (req.getEngineCapacity() != null) vehicle.setEngineCapacity(req.getEngineCapacity());
                    if (req.getHorsepower() != null) vehicle.setHorsepower(req.getHorsepower());
                    if (req.getDoors() != null) vehicle.setDoors(req.getDoors());
                    if (req.getSeats() != null) vehicle.setSeats(req.getSeats());
                    if (req.getListingMode() != null) vehicle.setListingMode(req.getListingMode());
                    if (req.getSalePrice() != null) vehicle.setSalePrice(req.getSalePrice());
                    if (req.getRentalPricePerDay() != null) vehicle.setRentalPricePerDay(req.getRentalPricePerDay());
                    if (req.getStockQuantity() != null) vehicle.setStockQuantity(req.getStockQuantity());
                    if (req.getStatus() != null) vehicle.setStatus(req.getStatus());
                    if (req.getDescription() != null) vehicle.setDescription(req.getDescription());
                    if (req.getFeatures() != null) vehicle.setFeatures(req.getFeatures());
                    if (req.getIsFeatured() != null) vehicle.setIsFeatured(req.getIsFeatured());
                    
                    vehicle.setUpdatedAt(Instant.now());
                    return vehicleRepository.save(vehicle);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));
    }

    public Mono<Void> deleteVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    // Suppression douce (Soft Delete)
                    vehicle.setIsActive(false);
                    vehicle.setUpdatedAt(Instant.now());
                    return vehicleRepository.save(vehicle);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .then();
    }
}
