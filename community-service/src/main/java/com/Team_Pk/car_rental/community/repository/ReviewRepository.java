package com.Team_Pk.car_rental.community.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.community.dto.RatingStats;
import com.Team_Pk.car_rental.community.entity.Review;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReviewRepository extends ReactiveCrudRepository<Review, UUID> {

    // 🔧 Utilisation de @Query custom avec CAST explicite pour éviter les erreurs ENUM vs VARCHAR
    @Query("SELECT * FROM community.reviews " +
           "WHERE entity_type = CAST(:entityType AS community.review_entity_type) " +
           "AND entity_id = :entityId " +
           "AND status = CAST(:status AS community.review_status) " +
           "ORDER BY created_at DESC")
    Flux<Review> findByEntityTypeAndEntityIdAndStatusOrderByCreatedAtDesc(String entityType, UUID entityId, String status);
    
    Flux<Review> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT * FROM community.reviews WHERE status = CAST(:status AS community.review_status) ORDER BY created_at DESC")
    Flux<Review> findByStatusOrderByCreatedAtDesc(String status);
    
    @Query("SELECT COUNT(*) FROM community.reviews WHERE status = CAST(:status AS community.review_status)")
    Mono<Long> countByStatus(String status);

    // Lecture depuis la vue matérialisée pour les statistiques (Pour Catalog-Service)
    @Query("SELECT average_rating, review_count FROM community.entity_ratings " +
           "WHERE entity_type = CAST(:entityType AS community.review_entity_type) " +
           "AND entity_id = :entityId")
    Mono<RatingStats> getStatsFromView(String entityType, UUID entityId);

    // Rafraîchissement de la vue matérialisée après approbation
    @Modifying
    @Query("REFRESH MATERIALIZED VIEW CONCURRENTLY community.entity_ratings")
    Mono<Void> refreshEntityRatings();
}