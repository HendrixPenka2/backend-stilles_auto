package com.Team_Pk.car_rental.auth.repository;

import com.Team_Pk.car_rental.auth.entity.RefreshToken;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, UUID> {
    Mono<RefreshToken> findByToken(String token);
    Mono<RefreshToken> findByUserId(UUID userId);
    Mono<Void> deleteByUserId(UUID userId);
}