package com.Team_Pk.car_rental.auth.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.auth.entity.EmailVerificationToken;

import reactor.core.publisher.Mono;

public interface EmailVerificationTokenRepository extends ReactiveCrudRepository<EmailVerificationToken, UUID> {
    // Conseil Pro : on cherche le token associé à l'ID de l'utilisateur précis
    Mono<EmailVerificationToken> findByUserIdAndToken(UUID userId, String token);
    
    // Pour nettoyer les anciens codes après réussite
    Mono<Void> deleteByUserId(UUID userId);
}