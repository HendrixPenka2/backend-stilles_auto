package com.Team_Pk.car_rental.community.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.community.entity.WishlistItem;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface WishlistRepository extends ReactiveCrudRepository<WishlistItem, UUID> {
    
    Flux<WishlistItem> findByUserIdOrderByAddedAtDesc(UUID userId);
    
    // 🔧 Utilisation de @Query custom avec CAST explicite pour éviter les erreurs ENUM vs VARCHAR
    @Query("SELECT EXISTS(SELECT 1 FROM community.wishlist_items " +
           "WHERE user_id = :userId " +
           "AND entity_type = CAST(:entityType AS community.wishlist_entity_type) " +
           "AND entity_id = :entityId)")
    Mono<Boolean> existsByUserIdAndEntityTypeAndEntityId(UUID userId, String entityType, UUID entityId);
    
    Mono<Void> deleteByUserIdAndId(UUID userId, UUID id);
    Mono<Void> deleteByUserId(UUID userId);
}