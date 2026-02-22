package com.Team_Pk.car_rental.auth.repository;

import com.Team_Pk.car_rental.auth.entity.PasswordResetToken;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PasswordResetTokenRepository extends R2dbcRepository<PasswordResetToken, UUID> {
    Mono<PasswordResetToken> findByToken(String token);
    Mono<PasswordResetToken> findByUserId(UUID userId);
    Mono<Void> deleteByUserId(UUID userId);
}