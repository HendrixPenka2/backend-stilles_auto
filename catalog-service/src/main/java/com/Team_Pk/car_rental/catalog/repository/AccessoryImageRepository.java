package com.Team_Pk.car_rental.catalog.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.catalog.entity.AccessoryImage;

import org.springframework.data.r2dbc.repository.Modifying;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AccessoryImageRepository extends ReactiveCrudRepository<AccessoryImage, UUID> {
    Flux<AccessoryImage> findByAccessoryIdOrderByDisplayOrderAsc(UUID accessoryId);
    Mono<Long> countByAccessoryId(UUID accessoryId);

    @Modifying
    @Query("UPDATE catalog.accessory_images SET is_primary = false WHERE accessory_id = :accessoryId")
    Mono<Integer> resetPrimaryStatus(UUID accessoryId);

    @Modifying
    @Query("UPDATE catalog.accessory_images SET display_order = :displayOrder, is_primary = :isPrimary WHERE id = :imageId AND accessory_id = :accessoryId")
    Mono<Integer> updateImageOrderAndPrimary(UUID imageId, UUID accessoryId, Integer displayOrder, Boolean isPrimary);
}