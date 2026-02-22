package com.Team_Pk.car_rental.catalog.service;


import com.Team_Pk.car_rental.catalog.dto.AvailabilityResponse;
import com.Team_Pk.car_rental.catalog.dto.BlockPeriodRequest;
import com.Team_Pk.car_rental.catalog.dto.FilterOptionsResponse;
import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentRequest;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleCalendarResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleDetailResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.StockMovement;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import com.Team_Pk.car_rental.catalog.entity.enums.StockMovementReason;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleStatus;
import com.Team_Pk.car_rental.catalog.repository.RentalAvailabilityRepository;
import com.Team_Pk.car_rental.catalog.repository.StockMovementRepository;
import com.Team_Pk.car_rental.catalog.repository.VehicleImageRepository;
import com.Team_Pk.car_rental.catalog.repository.VehicleRepository;
import com.Team_Pk.car_rental.catalog.dto.VehicleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository imageRepository;
    private final RentalAvailabilityRepository availabilityRepository;
    private final StockMovementRepository stockMovementRepository;
    

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
    // NOUVELLES MÉTHODES PUBLIQUES
    // ==========================================

    // 1. Les véhicules en vedette (Featured)
    public Mono<List<Vehicle>> getFeaturedVehicles(Integer limit) {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria();
        criteria.setIsFeatured(true);
        criteria.setPage(1);
        criteria.setLimit(limit != null ? limit : 6); // 6 par défaut pour le carrousel

        // On réutilise notre méthode searchVehicles !
        return vehicleRepository.findVehiclesByCriteria(criteria).collectList();
    }

    // 2. Les options de filtres dynamiques pour le front (ex: toutes les marques, tous les modèles, etc.)
    public Mono<FilterOptionsResponse> getFilterOptions() {
        return vehicleRepository.getFilterOptions();
    }


    // ==========================================
    // MÉTHODES ADMIN
    // ==========================================

    // ==============================================================
    // 1. CRÉATION AVEC STOCK INITIAL (INITIAL_STOCK)
    // ==============================================================
    @Transactional
    public Mono<Vehicle> createVehicle(VehicleRequest req, UUID adminId) {
        return vehicleRepository.existsByVin(req.getVin())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new RuntimeException("Ce VIN existe déjà."));

                    int initialStock = req.getStockQuantity() != null ? req.getStockQuantity() : 1;

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
                            .stockQuantity(initialStock) // On set le stock initial
                            .status(req.getStatus() != null ? req.getStatus() : VehicleStatus.AVAILABLE)
                            .description(req.getDescription())
                            .features(req.getFeatures())
                            .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                            .isActive(true)
                            .createdBy(adminId)
                            .build();

                    return vehicleRepository.save(vehicle)
                            .flatMap(savedVehicle -> {
                                // CRÉATION DU MOUVEMENT DE STOCK INITIAL
                                StockMovement movement = StockMovement.builder()
                                        .entityType("VEHICLE")
                                        .entityId(savedVehicle.getId())
                                        .quantityDelta(initialStock) // +1, +5, etc.
                                        .quantityBefore(0)
                                        .quantityAfter(initialStock)
                                        .reason(StockMovementReason.INITIAL_STOCK)
                                        .notes("Création initiale du véhicule")
                                        .performedBy(adminId)
                                        .createdAt(Instant.now())
                                        .build();

                                // On sauvegarde le mouvement, puis on retourne le véhicule
                                return stockMovementRepository.save(movement).thenReturn(savedVehicle);
                            });
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

        // ==============================================================
    // 2. AJUSTEMENT MANUEL DU STOCK (PATCH /stock)
    // ==============================================================
    @Transactional
    public Mono<StockAdjustmentResponse> adjustVehicleStock(UUID id, StockAdjustmentRequest req, UUID adminId) {
        return vehicleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .flatMap(vehicle -> {
                    int oldQty = vehicle.getStockQuantity();
                    int newQty = oldQty + req.getQuantityDelta();

                    if (newQty < 0) {
                        return Mono.error(new RuntimeException("Le stock ne peut pas être négatif"));
                    }

                    // Mise à jour du véhicule
                    vehicle.setStockQuantity(newQty);
                    vehicle.setUpdatedAt(Instant.now());

                    return vehicleRepository.save(vehicle)
                            .flatMap(savedVehicle -> {
                                // CRÉATION DU MOUVEMENT D'AJUSTEMENT
                                StockMovement movement = StockMovement.builder()
                                        .entityType("VEHICLE")
                                        .entityId(savedVehicle.getId())
                                        .quantityDelta(req.getQuantityDelta())
                                        .quantityBefore(oldQty)
                                        .quantityAfter(newQty)
                                        .reason(req.getReason()) // PURCHASE_IN, LOSS, ADJUSTMENT...
                                        .notes(req.getNotes())
                                        .performedBy(adminId)
                                        .createdAt(Instant.now())
                                        .build();

                                return stockMovementRepository.save(movement)
                                        .map(savedMov -> StockAdjustmentResponse.builder()
                                                .entityId(savedVehicle.getId()) // ID générique (nom du champ DTO peut varier)
                                                .quantityBefore(oldQty)
                                                .quantityAfter(newQty)
                                                .movementId(savedMov.getId())
                                                .build());
                            });
                });
    }

    // ==============================================================
    // 3. SUPPRESSION / DÉSACTIVATION (ADJUSTMENT - Sortie totale)
    // ==============================================================
    @Transactional
    public Mono<Void> deleteVehicle(UUID id, UUID adminId) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    int currentStock = vehicle.getStockQuantity();
                    
                    // On désactive le véhicule
                    vehicle.setIsActive(false);
                    // On remet le stock à 0 pour qu'il ne soit plus compté
                    vehicle.setStockQuantity(0);
                    vehicle.setUpdatedAt(Instant.now());

                    return vehicleRepository.save(vehicle)
                            .flatMap(saved -> {
                                // Si on avait du stock, on enregistre qu'on l'a retiré
                                if (currentStock > 0) {
                                    StockMovement movement = StockMovement.builder()
                                            .entityType("VEHICLE")
                                            .entityId(saved.getId())
                                            .quantityDelta(-currentStock) // On retire tout (-X)
                                            .quantityBefore(currentStock)
                                            .quantityAfter(0)
                                            .reason(StockMovementReason.ADJUSTMENT)
                                            .notes("Désactivation du véhicule (Sortie de stock)")
                                            .performedBy(adminId)
                                            .createdAt(Instant.now())
                                            .build();
                                    return stockMovementRepository.save(movement);
                                }
                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .then();
    }

    // ==========================================
    // NOUVELLES MÉTHODES ADMIN
    // ==========================================

    // 3. Liste Admin (inclut les véhicules inactifs + permet la recherche par mot-clé)
    public Mono<PaginatedResponse<Vehicle>> getAdminVehicles(VehicleSearchCriteria criteria) {
        // Pour l'admin, on veut voir TOUS les véhicules (actifs ET inactifs)
        // mais avec la possibilité de faire des recherches par mot-clé
        Mono<List<Vehicle>> dataMono = vehicleRepository.findAllVehiclesByCriteria(criteria).collectList();
        Mono<Long> countMono = vehicleRepository.countAllVehiclesByCriteria(criteria);

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

    // 4. Réactiver un véhicule désactivé
    public Mono<Vehicle> reactivateVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    if (vehicle.getIsActive()) {
                        return Mono.error(new RuntimeException("Ce véhicule est déjà actif"));
                    }
                    vehicle.setIsActive(true);
                    vehicle.setUpdatedAt(Instant.now());
                    return vehicleRepository.save(vehicle);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));
    }

    // ==========================================
    // DISPONIBILITÉS (CALENDRIER)
    // ==========================================

// ==========================================
    // MISE À JOUR : CALCUL DU CALENDRIER
    // ==========================================

    public Mono<VehicleCalendarResponse> getVehicleAvailability(UUID vehicleId, LocalDate from, LocalDate to) {
        // Par défaut, on prend le mois en cours si from/to sont null
        LocalDate start = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = (to != null) ? to : start.plusMonths(1).withDayOfMonth(start.plusMonths(1).lengthOfMonth());

        return vehicleRepository.findById(vehicleId)
                .flatMap(vehicle -> 
                    availabilityRepository.getFutureAvailability(vehicleId)
                        .collectList()
                        .map(blocked -> {
                            // Ici on calcule les trous (périodes libres)
                            List<VehicleCalendarResponse.Period> available = calculateAvailablePeriods(start, end, blocked);
                            
                            return VehicleCalendarResponse.builder()
                                    .vehicleId(vehicle.getId())
                                    .rentalPricePerDay(vehicle.getRentalPricePerDay())
                                    .blockedPeriods(blocked)
                                    .availablePeriods(available)
                                    .build();
                        })
                );
    }

    // Algorithme simple pour trouver les trous
    private List<VehicleCalendarResponse.Period> calculateAvailablePeriods(LocalDate start, LocalDate end, List<AvailabilityResponse> blocked) {
        List<VehicleCalendarResponse.Period> available = new java.util.ArrayList<>();
        LocalDate current = start;

        // On trie les blocages par date
        blocked.sort((a, b) -> a.getStartDate().compareTo(b.getStartDate()));

        for (AvailabilityResponse block : blocked) {
            // Si le blocage commence après notre curseur, il y a un trou (donc dispo !)
            if (block.getStartDate().isAfter(current)) {
                available.add(VehicleCalendarResponse.Period.builder()
                        .start(current)
                        .end(block.getStartDate().minusDays(1))
                        .build());
            }
            // On avance le curseur après le blocage
            if (block.getEndDate().isAfter(current)) {
                current = block.getEndDate().plusDays(1);
            }
        }

        // S'il reste du temps après le dernier blocage jusqu'à la fin demandée
        if (current.isBefore(end) || current.equals(end)) {
            available.add(VehicleCalendarResponse.Period.builder()
                    .start(current)
                    .end(end)
                    .build());
        }
        return available;
    }

    // ADMIN : Bloquer manuellement (ex: La voiture va au garage)
    public Mono<UUID> blockVehiclePeriod(UUID vehicleId, BlockPeriodRequest req) {
        if (req.getStartDate().isAfter(req.getEndDate())) {
            return Mono.error(new RuntimeException("La date de début doit être avant la date de fin"));
        }
        
        // On intercepte l'erreur GIST de PostgreSQL (Double booking)
        return availabilityRepository.blockPeriod(
                vehicleId, req.getStartDate(), req.getEndDate(), req.getReason(), req.getNotes()
        ).onErrorMap(e -> new RuntimeException("Conflit de dates : Ce véhicule est déjà indisponible sur cette période !"));
    }

    // ADMIN : Débloquer
    public Mono<Void> unblockVehiclePeriod(UUID vehicleId, UUID blockId) {
        return availabilityRepository.deleteBlock(blockId, vehicleId);
    }

    // ==========================================
    // STOCK
    // ==========================================

    public Flux<StockMovement> getVehicleStockHistory(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .thenMany(stockMovementRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("VEHICLE", vehicleId));
    }
}
