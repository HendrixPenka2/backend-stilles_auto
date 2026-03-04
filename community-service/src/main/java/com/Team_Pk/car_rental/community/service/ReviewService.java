package com.Team_Pk.car_rental.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Team_Pk.car_rental.community.config.RabbitMQConfig;
import com.Team_Pk.car_rental.community.dto.ReviewEvent;
import com.Team_Pk.car_rental.community.dto.ReviewRequest;
import com.Team_Pk.car_rental.community.dto.ReviewStatsResponse;
import com.Team_Pk.car_rental.community.entity.Review;
import com.Team_Pk.car_rental.community.entity.enums.ReviewStatus;
import com.Team_Pk.car_rental.community.repository.ReviewRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public Mono<Review> submitReview(ReviewRequest req, UUID userId) {
        Review review = Review.builder()
                .userId(userId).entityType(req.getEntityType()).entityId(req.getEntityId())
                .rating(req.getRating()).title(req.getTitle()).comment(req.getComment())
                .status(ReviewStatus.PENDING).createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        return reviewRepository.save(review)
                .doOnSuccess(saved -> sendRabbitEvent(saved, RabbitMQConfig.REVIEW_SUBMITTED_KEY));
    }

    public Flux<Review> getMyReviews(UUID userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public Flux<Review> getPublicReviews(String entityType, UUID entityId) {
        return reviewRepository.findByEntityTypeAndEntityIdAndStatusOrderByCreatedAtDesc(
                entityType.toUpperCase(), entityId, "APPROVED");
    }

    @Transactional
    public Mono<Review> updateReview(UUID reviewId, ReviewRequest req, UUID userId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new RuntimeException("Avis introuvable ou non autorisé")))
                .flatMap(review -> {
                    review.setRating(req.getRating());
                    review.setTitle(req.getTitle());
                    review.setComment(req.getComment());
                    review.setStatus(ReviewStatus.PENDING); // Repasse en validation admin !
                    review.setUpdatedAt(Instant.now());
                    return reviewRepository.save(review)
                            .doOnSuccess(saved -> sendRabbitEvent(saved, RabbitMQConfig.REVIEW_SUBMITTED_KEY));
                });
    }

    @Transactional
    public Mono<Void> deleteReview(UUID reviewId, UUID userId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getUserId().equals(userId))
                .flatMap(reviewRepository::delete);
    }

    // --- ADMIN ---

    public Flux<Review> getAllReviews(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return reviewRepository.findByStatusOrderByCreatedAtDesc(statusFilter.toUpperCase());
        }
        return reviewRepository.findAll();
    }

    @Transactional
    public Mono<Review> moderateReview(UUID reviewId, ReviewStatus newStatus, String adminNote, UUID adminId) {
        return reviewRepository.findById(reviewId)
                .switchIfEmpty(Mono.error(new RuntimeException("Avis introuvable")))
                .flatMap(review -> {
                    review.setStatus(newStatus);
                    review.setAdminNote(adminNote);
                    review.setModeratedBy(adminId);
                    review.setModeratedAt(Instant.now());
                    review.setUpdatedAt(Instant.now());

                    return reviewRepository.save(review)
                            .flatMap(saved -> {
                                if (newStatus == ReviewStatus.APPROVED) {
                                    return reviewRepository.refreshEntityRatings()
                                            .then(Mono.fromRunnable(() -> sendRabbitEvent(saved, RabbitMQConfig.REVIEW_APPROVED_KEY)))
                                            .thenReturn(saved);
                                } else if (newStatus == ReviewStatus.REJECTED) {
                                    sendRabbitEvent(saved, RabbitMQConfig.REVIEW_REJECTED_KEY);
                                }
                                return Mono.just(saved);
                            });
                });
    }
    
    public Mono<ReviewStatsResponse> getStats() {
        return Mono.zip(reviewRepository.count(), reviewRepository.countByStatus("PENDING"))
            .map(t -> ReviewStatsResponse.builder().totalReviews(t.getT1()).pendingCount(t.getT2()).averageGlobalRating(0.0).build());
    }

    private void sendRabbitEvent(Review review, String routingKey) {
        try {
            ReviewEvent event = ReviewEvent.builder()
                    .reviewId(review.getId()).userId(review.getUserId())
                    .entityType(review.getEntityType().name()).entityId(review.getEntityId())
                    .rating(review.getRating()).status(review.getStatus().name()).build();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
            log.info("Event envoyé: {}", routingKey);
        } catch (Exception e) {
            log.error("Echec envoi RabbitMQ", e);
        }
    }
}