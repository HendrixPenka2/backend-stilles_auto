package com.Team_Pk.car_rental.catalog.service;

import com.Team_Pk.car_rental.catalog.dto.*;
import com.Team_Pk.car_rental.catalog.entity.StockMovement;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import com.Team_Pk.car_rental.catalog.entity.enums.ListingMode;
import com.Team_Pk.car_rental.catalog.entity.enums.StockMovementReason;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleStatus;
import com.Team_Pk.car_rental.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository imageRepository;
    private final RentalAvailabilityRepository availabilityRepository;
    private final StockMovementRepository stockMovementRepository;

    // NOUVEAU : On injecte notre client HTTP
    private final CommunityWebClient communityWebClient; 

    // ==============================================================
    // 1. LECTURE PUBLIQUE (API FRONTEND CLIENT)
    // ==============================================================

    /**
     * Recherche paginée avec filtres dynamiques (Pattern BFF)
     * POINT BIZARRE : On lance les requêtes de données et de comptage EN PARALLÈLE avec Mono.zip
     * pour optimiser le temps de réponse.
     */
    public Mono<PaginatedResponse<Vehicle>> searchVehicles(VehicleSearchCriteria criteria) {
        Mono<List<Vehicle>> dataMono = vehicleRepository.findVehiclesByCriteria(criteria).collectList();
        Mono<Long> countMono = vehicleRepository.countVehiclesByCriteria(criteria);

        return Mono.zip(dataMono, countMono).map(tuple -> {
            List<Vehicle> data = tuple.getT1();
            long total = tuple.getT2();
            int totalPages = (int) Math.ceil((double) total / criteria.getLimit());

            PaginatedResponse.Meta meta = PaginatedResponse.Meta.builder()
                    .total(total).page(criteria.getPage()).limit(criteria.getLimit())
                    .totalPages(totalPages).hasNext(criteria.getPage() < totalPages)
                    .hasPrev(criteria.getPage() > 1).build();

            return PaginatedResponse.<Vehicle>builder().data(data).meta(meta).build();
        });
    }

    /**
     * Récupère un véhicule avec ses images
     */
    public Mono<VehicleDetailResponse> getVehicleById(UUID id) {
        Mono<Vehicle> vehicleMono = vehicleRepository.findById(id)
                .filter(Vehicle::getIsActive) // Sécurité : on cache les véhicules supprimés
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));

        Mono<List<VehicleImage>> imagesMono = imageRepository
                .findByVehicleIdOrderByDisplayOrderAsc(id).collectList();

        // 3. NOUVEAU : Cherche la note dans Community-Service
        Mono<CommunityWebClient.RatingDto> ratingMono = communityWebClient.getEntityRating("VEHICLE", id);

        // 4. On combine les 3 (Mono.zip prend jusqu'à 8 Monos !)
        return Mono.zip(vehicleMono, imagesMono, ratingMono).map(tuple -> 
                VehicleDetailResponse.builder()
                        .vehicle(tuple.getT1())
                        .images(tuple.getT2())
                        .averageRating(tuple.getT3().getAverageRating()) // Vraie donnée
                        .reviewCount(tuple.getT3().getReviewCount())     // Vraie donnée
                        .build()
        );
    }

    public Mono<List<Vehicle>> getFeaturedVehicles(Integer limit) {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria();
        criteria.setIsFeatured(true);
        criteria.setPage(1);
        criteria.setLimit(limit != null ? limit : 6); 
        return vehicleRepository.findVehiclesByCriteria(criteria).collectList();
    }

    public Mono<FilterOptionsResponse> getFilterOptions() {
        return vehicleRepository.getFilterOptions();
    }

    // ==============================================================
    // 2. GESTION VÉHICULES (ADMIN) - RÈGLES MÉTIER STRICTES
    // ==============================================================

    /**
     * Création d'un véhicule. Intègre la vérification stricte proposée pour la location.
     */
    @Transactional
    public Mono<Vehicle> createVehicle(VehicleRequest req, UUID adminId) {
        // RÈGLE MÉTIER ABSOLUE (Proposée par l'architecte) :
        // On interdit la création d'un "Template" (Stock > 1) pour un véhicule destiné à la location.
        if ((req.getListingMode() == ListingMode.RENTAL_ONLY || req.getListingMode() == ListingMode.BOTH)
                && (req.getVin() == null || req.getVin().isBlank())
                && (req.getStockQuantity() != null && req.getStockQuantity() > 1)) {
            return Mono.error(new RuntimeException("Pour la location, chaque véhicule doit avoir un VIN unique et un stock de 1. Créez une fiche par véhicule."));
        }

        // Vérification unicité du VIN (Seulement s'il est renseigné)
        Mono<Boolean> vinCheck = (req.getVin() != null && !req.getVin().isBlank()) 
                ? vehicleRepository.existsByVin(req.getVin()) 
                : Mono.just(false);

        return vinCheck.flatMap(exists -> {
            if (exists) return Mono.error(new RuntimeException("Ce VIN existe déjà dans le système."));

            // Si VIN renseigné -> Vraie voiture -> Stock forcé à 1
            // Si pas de VIN (Template Vente) -> Stock demandé
            int initialStock = (req.getVin() != null && !req.getVin().isBlank()) 
                    ? 1 
                    : (req.getStockQuantity() != null ? req.getStockQuantity() : 0);

            Vehicle vehicle = Vehicle.builder()
                    .title(req.getTitle()).brand(req.getBrand()).model(req.getModel())
                    .year(req.getYear()).color(req.getColor()).vin(req.getVin())
                    .vehicleType(req.getVehicleType()).fuelType(req.getFuelType())
                    .transmission(req.getTransmission()).mileage(req.getMileage())
                    .engineCapacity(req.getEngineCapacity()).horsepower(req.getHorsepower())
                    .doors(req.getDoors()).seats(req.getSeats()).listingMode(req.getListingMode())
                    .salePrice(req.getSalePrice()).rentalPricePerDay(req.getRentalPricePerDay())
                    .stockQuantity(initialStock) 
                    .status(req.getStatus() != null ? req.getStatus() : VehicleStatus.AVAILABLE)
                    .description(req.getDescription()).features(req.getFeatures())
                    .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                    .isActive(true).createdBy(adminId).build();

            return vehicleRepository.save(vehicle)
                    .flatMap(savedVehicle -> {
                        // Création de l'audit de stock
                        String notes = (req.getVin() == null || req.getVin().isBlank()) 
                                ? "Création d'un lot/template sans VIN" 
                                : "Création d'un véhicule physique unique";
                        StockMovement movement = createMovement(savedVehicle.getId(), initialStock, 0, initialStock, StockMovementReason.INITIAL_STOCK, notes, adminId);
                        return stockMovementRepository.save(movement).thenReturn(savedVehicle);
                    });
        });
    }

    /**
     * Mise à jour globale d'un véhicule (Gère aussi le changement de Mode et de Stock)
     */
    @Transactional
    public Mono<Vehicle> updateVehicle(UUID id, VehicleRequest req, UUID adminId) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    
                    // ON VÉRIFIE LES RÈGLES DE SÉCURITÉ SI L'ADMIN CHANGE LE MODE OU LE STOCK
                    ListingMode newMode = req.getListingMode() != null ? req.getListingMode() : vehicle.getListingMode();
                    int newStock = req.getStockQuantity() != null ? req.getStockQuantity() : vehicle.getStockQuantity();
                    String currentVin = req.getVin() != null ? req.getVin() : vehicle.getVin();

                    // Règle 1 : Pas de template (Stock > 1) pour la location
                    if ((newMode == ListingMode.RENTAL_ONLY || newMode == ListingMode.BOTH) && (currentVin == null || currentVin.isBlank()) && newStock > 1) {
                        return Mono.error(new RuntimeException("Impossible : Un véhicule en location ne peut pas être un template avec un stock > 1."));
                    }
                    // Règle 2 : Un vrai véhicule avec VIN = stock max 1
                    if (currentVin != null && !currentVin.isBlank() && newStock > 1) {
                        return Mono.error(new RuntimeException("Un véhicule physique (avec VIN) ne peut pas avoir plus de 1 en stock."));
                    }

                    // POINT BIZARRE : stockAuditMono stocke la transaction d'historique s'il y a un changement de stock.
                    // Sinon, il renvoie Mono.empty() pour ne rien faire.
                    Mono<Void> stockAuditMono = Mono.empty();
                    if (req.getStockQuantity() != null && !req.getStockQuantity().equals(vehicle.getStockQuantity())) {
                        int diff = req.getStockQuantity() - vehicle.getStockQuantity();
                        StockMovement movement = createMovement(vehicle.getId(), diff, vehicle.getStockQuantity(), req.getStockQuantity(), StockMovementReason.ADJUSTMENT, "Modification via fiche produit", adminId);
                        stockAuditMono = stockMovementRepository.save(movement).then();
                        vehicle.setStockQuantity(req.getStockQuantity());
                    }

                    // Application du patch (mise à jour partielle)
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
                    if (req.getStatus() != null) vehicle.setStatus(req.getStatus());
                    if (req.getDescription() != null) vehicle.setDescription(req.getDescription());
                    if (req.getFeatures() != null) vehicle.setFeatures(req.getFeatures());
                    if (req.getIsFeatured() != null) vehicle.setIsFeatured(req.getIsFeatured());
                    
                    vehicle.setUpdatedAt(Instant.now());
                    
                    // .then() s'assure qu'on sauvegarde le mouvement AVANT de sauvegarder le véhicule
                    return stockAuditMono.then(vehicleRepository.save(vehicle));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));
    }

    /**
     * Ajustement spécifique du stock via un bouton dédié sur le dashboard
     */
    @Transactional
    public Mono<StockAdjustmentResponse> adjustVehicleStock(UUID id, StockAdjustmentRequest req, UUID adminId) {
        return vehicleRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .flatMap(vehicle -> {
                    int oldQty = vehicle.getStockQuantity();
                    int newQty = oldQty + req.getQuantityDelta();

                    if (newQty < 0) return Mono.error(new RuntimeException("Le stock ne peut pas être négatif"));

                    // APPLICATION DES RÈGLES DE SÉCURITÉ
                    if (vehicle.getVin() != null && !vehicle.getVin().isBlank() && newQty > 1) {
                        return Mono.error(new RuntimeException("Un véhicule physique (VIN) ne peut pas avoir un stock > 1."));
                    }
                    if ((vehicle.getListingMode() == ListingMode.RENTAL_ONLY || vehicle.getListingMode() == ListingMode.BOTH) && newQty > 1) {
                        return Mono.error(new RuntimeException("Un véhicule louable ne peut pas avoir un stock > 1."));
                    }

                    vehicle.setStockQuantity(newQty);
                    vehicle.setUpdatedAt(Instant.now());

                    return vehicleRepository.save(vehicle)
                            .flatMap(savedVehicle -> {
                                StockMovement movement = createMovement(savedVehicle.getId(), req.getQuantityDelta(), oldQty, newQty, req.getReason(), req.getNotes(), adminId);
                                return stockMovementRepository.save(movement)
                                        .map(savedMov -> StockAdjustmentResponse.builder()
                                                .entityId(savedVehicle.getId())
                                                .quantityBefore(oldQty).quantityAfter(newQty).movementId(savedMov.getId())
                                                .build());
                            });
                });
    }

    /**
     * Soft Delete : On désactive et on vide le stock (avec trace)
     */
    @Transactional
    public Mono<Void> deleteVehicle(UUID id, UUID adminId) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    int currentStock = vehicle.getStockQuantity();
                    vehicle.setIsActive(false);
                    vehicle.setStockQuantity(0);
                    vehicle.setUpdatedAt(Instant.now());

                    return vehicleRepository.save(vehicle)
                            .flatMap(saved -> {
                                if (currentStock > 0) {
                                    StockMovement movement = createMovement(saved.getId(), -currentStock, currentStock, 0, StockMovementReason.ADJUSTMENT, "Désactivation (Retrait du stock)", adminId);
                                    return stockMovementRepository.save(movement);
                                }
                                return Mono.empty();
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .then();
    }

    public Mono<Vehicle> reactivateVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .flatMap(vehicle -> {
                    if (vehicle.getIsActive()) return Mono.error(new RuntimeException("Ce véhicule est déjà actif"));
                    vehicle.setIsActive(true);
                    vehicle.setUpdatedAt(Instant.now());
                    return vehicleRepository.save(vehicle);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")));
    }

    public Mono<PaginatedResponse<Vehicle>> getAdminVehicles(VehicleSearchCriteria criteria) {
        Mono<List<Vehicle>> dataMono = vehicleRepository.findAllVehiclesByCriteria(criteria).collectList();
        Mono<Long> countMono = vehicleRepository.countAllVehiclesByCriteria(criteria);

        return Mono.zip(dataMono, countMono).map(tuple -> {
            int totalPages = (int) Math.ceil((double) tuple.getT2() / criteria.getLimit());
            PaginatedResponse.Meta meta = PaginatedResponse.Meta.builder()
                    .total(tuple.getT2()).page(criteria.getPage()).limit(criteria.getLimit())
                    .totalPages(totalPages).hasNext(criteria.getPage() < totalPages)
                    .hasPrev(criteria.getPage() > 1).build();
            return PaginatedResponse.<Vehicle>builder().data(tuple.getT1()).meta(meta).build();
        });
    }

    // ==============================================================
    // 3. DISPONIBILITÉS (CALENDRIER DE LOCATION)
    // ==============================================================

    /**
     * POINT BIZARRE : Pour afficher le calendrier, on a besoin des dates bloquées, 
     * MAIS AUSSI des "trous" entre ces dates (qui sont les disponibilités réelles).
     * On calcule les trous manuellement dans calculateAvailablePeriods.
     */
    public Mono<VehicleCalendarResponse> getVehicleAvailability(UUID vehicleId, LocalDate from, LocalDate to) {
        LocalDate start = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = (to != null) ? to : start.plusMonths(1).withDayOfMonth(start.plusMonths(1).lengthOfMonth());

        return vehicleRepository.findById(vehicleId)
                .flatMap(vehicle -> 
                    availabilityRepository.getFutureAvailability(vehicleId)
                        .collectList()
                        .map(blocked -> {
                            List<VehicleCalendarResponse.Period> available = calculateAvailablePeriods(start, end, blocked);
                            return VehicleCalendarResponse.builder()
                                    .vehicleId(vehicle.getId()).rentalPricePerDay(vehicle.getRentalPricePerDay())
                                    .blockedPeriods(blocked).availablePeriods(available).build();
                        })
                );
    }

    private List<VehicleCalendarResponse.Period> calculateAvailablePeriods(LocalDate start, LocalDate end, List<AvailabilityResponse> blocked) {
        List<VehicleCalendarResponse.Period> available = new ArrayList<>();
        LocalDate current = start;

        blocked.sort((a, b) -> a.getStartDate().compareTo(b.getStartDate()));

        for (AvailabilityResponse block : blocked) {
            if (block.getStartDate().isAfter(current)) {
                available.add(VehicleCalendarResponse.Period.builder().start(current).end(block.getStartDate().minusDays(1)).build());
            }
            if (block.getEndDate().isAfter(current)) current = block.getEndDate().plusDays(1);
        }

        if (current.isBefore(end) || current.equals(end)) {
            available.add(VehicleCalendarResponse.Period.builder().start(current).end(end).build());
        }
        return available;
    }

    /**
     * POINT BIZARRE : PostgreSQL gère l'anti-double booking via "EXCLUDE USING GIST".
     * Si l'admin ajoute une date qui chevauche une autre, la BDD pète une erreur (DataIntegrityViolation).
     * On attrape cette erreur via onErrorMap pour renvoyer un message lisible au Frontend.
     */
    public Mono<UUID> blockVehiclePeriod(UUID vehicleId, BlockPeriodRequest req) {
        if (req.getStartDate().isAfter(req.getEndDate())) {
            return Mono.error(new RuntimeException("La date de début doit être avant la date de fin"));
        }
        return availabilityRepository.blockPeriod(vehicleId, req.getStartDate(), req.getEndDate(), req.getReason(), req.getNotes())
                .onErrorMap(e -> new RuntimeException("Conflit de dates : Ce véhicule est déjà indisponible sur cette période !"));
    }

    public Mono<Void> unblockVehiclePeriod(UUID vehicleId, UUID blockId) {
        return availabilityRepository.deleteBlock(blockId, vehicleId);
    }

    // ==============================================================
    // 4. HISTORIQUE DE STOCK ET HELPERS
    // ==============================================================

    public Mono<StockHistoryListResponse> getVehicleStockHistory(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .flatMap(vehicle -> 
                    stockMovementRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("VEHICLE", vehicleId)
                        .collectList()
                        .map(movements -> StockHistoryListResponse.builder()
                                .entityId(vehicle.getId())
                                .currentStock(vehicle.getStockQuantity())
                                .movements(movements)
                                .build())
                );
    }

    /**
     * Méthode utilitaire pour centraliser la création d'un log de mouvement de stock
     */
    private StockMovement createMovement(UUID entityId, int delta, int before, int after, StockMovementReason reason, String notes, UUID adminId) {
        return StockMovement.builder()
                .entityType("VEHICLE")
                .entityId(entityId)
                .quantityDelta(delta)
                .quantityBefore(before)
                .quantityAfter(after)
                .reason(reason)
                .notes(notes)
                .performedBy(adminId)
                .createdAt(Instant.now())
                .build();
    }
}