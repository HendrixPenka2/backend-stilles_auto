package com.Team_Pk.car_rental.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Team_Pk.car_rental.catalog.dto.AccessoryDetailResponse;
import com.Team_Pk.car_rental.catalog.dto.AccessoryFilterOptions;
import com.Team_Pk.car_rental.catalog.dto.AccessoryRequest;
import com.Team_Pk.car_rental.catalog.dto.AccessorySearchCriteria;
import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentRequest;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentResponse;
import com.Team_Pk.car_rental.catalog.entity.Accessory;
import com.Team_Pk.car_rental.catalog.entity.StockMovement;
import com.Team_Pk.car_rental.catalog.entity.enums.StockMovementReason;
import com.Team_Pk.car_rental.catalog.repository.AccessoryImageRepository;
import com.Team_Pk.car_rental.catalog.repository.AccessoryRepository;
import com.Team_Pk.car_rental.catalog.repository.StockMovementRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessoryService {

    private final AccessoryRepository accessoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final AccessoryImageRepository imageRepository;

    // --- PUBLIC ---
    public Mono<PaginatedResponse<Accessory>> searchAccessories(AccessorySearchCriteria c) {
        return Mono.zip(
            accessoryRepository.findAccessoriesByCriteria(c).collectList(),
            accessoryRepository.countAccessoriesByCriteria(c)
        ).map(tuple -> {
            List<Accessory> data = tuple.getT1();
            long total = tuple.getT2();
            int totalPages = (int) Math.ceil((double) total / c.getLimit());
            return PaginatedResponse.<Accessory>builder()
                    .data(data)
                    .meta(PaginatedResponse.Meta.builder().total(total).page(c.getPage()).limit(c.getLimit()).totalPages(totalPages).build())
                    .build();
        });
    }

    public Mono<AccessoryDetailResponse> getAccessoryById(UUID id) {
        return accessoryRepository.findById(id)
                .filter(Accessory::getIsActive)
                .switchIfEmpty(Mono.error(new RuntimeException("Accessoire introuvable")))
                .zipWith(imageRepository.findByAccessoryIdOrderByDisplayOrderAsc(id).collectList())
                .map(tuple -> AccessoryDetailResponse.builder()
                        .accessory(tuple.getT1())
                        .images(tuple.getT2())
                        .averageRating(0.0).reviewCount(0).build());
    }

    public Mono<AccessoryFilterOptions> getFilterOptions() {
        return accessoryRepository.getFilterOptions();
    }
    
    public Mono<List<Accessory>> getFeatured() {
        AccessorySearchCriteria c = new AccessorySearchCriteria();
        c.setIsFeatured(true);
        return accessoryRepository.findAccessoriesByCriteria(c).collectList();
    }

    // ==============================================================
    // 1. CRÉATION ACCESSOIRE (INITIAL_STOCK)
    // ==============================================================
    @Transactional
    public Mono<Accessory> createAccessory(AccessoryRequest req, UUID adminId) {
        int initialStock = req.getStockQuantity() != null ? req.getStockQuantity() : 0;

        Accessory accessory = Accessory.builder()
                .name(req.getName())
                .brand(req.getBrand())
                .sku(req.getSku())
                .category(req.getCategory())
                .subCategory(req.getSubCategory())
                .compatibleBrands(req.getCompatibleBrands())
                .compatibleModels(req.getCompatibleModels())
                .price(req.getPrice())
                .comparePrice(req.getComparePrice())
                .stockQuantity(initialStock) // Stock initial
                .lowStockAlert(req.getLowStockAlert() != null ? req.getLowStockAlert() : 5)
                .condition(req.getCondition())
                .weightKg(req.getWeightKg())
                .dimensions(req.getDimensions())
                .description(req.getDescription())
                .specifications(req.getSpecifications())
                .isFeatured(req.getIsFeatured() != null ? req.getIsFeatured() : false)
                .isActive(true)
                .createdBy(adminId)
                .build();

        return accessoryRepository.save(accessory)
                .flatMap(savedAcc -> {
                    // Audit du stock initial
                    StockMovement movement = StockMovement.builder()
                            .entityType("ACCESSORY")
                            .entityId(savedAcc.getId())
                            .quantityDelta(initialStock)
                            .quantityBefore(0)
                            .quantityAfter(initialStock)
                            .reason(StockMovementReason.INITIAL_STOCK)
                            .notes("Création initiale")
                            .performedBy(adminId)
                            .createdAt(Instant.now())
                            .build();
                    return stockMovementRepository.save(movement).thenReturn(savedAcc);
                });
    }

    


    public Mono<Accessory> updateAccessory(UUID id, AccessoryRequest req) {
        return accessoryRepository.findById(id)
                .flatMap(acc -> {
                    if (req.getName() != null) acc.setName(req.getName());
                    if (req.getPrice() != null) acc.setPrice(req.getPrice());
                    if (req.getIsActive() != null) acc.setIsActive(req.getIsActive());
                    // ... mapper les autres champs ...
                    acc.setUpdatedAt(Instant.now());
                    return accessoryRepository.save(acc);
                });
    }

    // ==============================================================
    // 3. DÉSACTIVATION ACCESSOIRE
    // ==============================================================
    @Transactional
    public Mono<Void> deleteAccessory(UUID id, UUID adminId) {
        return accessoryRepository.findById(id)
                .flatMap(acc -> {
                    int currentStock = acc.getStockQuantity();
                    
                    acc.setIsActive(false);
                    acc.setStockQuantity(0); // On vide le stock logique
                    
                    return accessoryRepository.save(acc)
                            .flatMap(saved -> {
                                if (currentStock > 0) {
                                    StockMovement movement = StockMovement.builder()
                                            .entityType("ACCESSORY")
                                            .entityId(saved.getId())
                                            .quantityDelta(-currentStock)
                                            .quantityBefore(currentStock)
                                            .quantityAfter(0)
                                            .reason(StockMovementReason.ADJUSTMENT)
                                            .notes("Désactivation (Sortie de stock)")
                                            .performedBy(adminId)
                                            .createdAt(Instant.now())
                                            .build();
                                    return stockMovementRepository.save(movement);
                                }
                                return Mono.empty();
                            });
                }).then();
    }
    // ==============================================================
    // 2. AJUSTEMENT MANUEL ACCESSOIRE (PATCH /stock)
    // ==============================================================
    @Transactional
    public Mono<StockAdjustmentResponse> adjustStock(UUID id, StockAdjustmentRequest req, UUID adminId) {
        return accessoryRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Accessoire introuvable")))
                .flatMap(acc -> {
                    int oldQty = acc.getStockQuantity();
                    int newQty = oldQty + req.getQuantityDelta();
                    
                    if (newQty < 0) return Mono.error(new RuntimeException("Stock insuffisant"));

                    acc.setStockQuantity(newQty);
                    acc.setUpdatedAt(Instant.now());

                    return accessoryRepository.save(acc)
                            .flatMap(savedAcc -> {
                                StockMovement movement = StockMovement.builder()
                                        .entityType("ACCESSORY")
                                        .entityId(savedAcc.getId())
                                        .quantityDelta(req.getQuantityDelta())
                                        .quantityBefore(oldQty)
                                        .quantityAfter(newQty)
                                        .reason(req.getReason())
                                        .notes(req.getNotes())
                                        .performedBy(adminId)
                                        .createdAt(Instant.now())
                                        .build();

                                return stockMovementRepository.save(movement)
                                        .map(savedMov -> StockAdjustmentResponse.builder()
                                                .entityId(savedAcc.getId())
                                                .quantityBefore(oldQty)
                                                .quantityAfter(newQty)
                                                .movementId(savedMov.getId())
                                                .build());
                            });
                });
    }

    public Flux<StockMovement> getStockHistory(UUID id) {
        return stockMovementRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("ACCESSORY", id);
    }
}