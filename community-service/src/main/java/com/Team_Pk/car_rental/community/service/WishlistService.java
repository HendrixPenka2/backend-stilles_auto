package com.Team_Pk.car_rental.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.Team_Pk.car_rental.community.dto.WishlistRequest;
import com.Team_Pk.car_rental.community.entity.WishlistItem;
import com.Team_Pk.car_rental.community.repository.WishlistRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    public Flux<WishlistItem> getMyWishlist(UUID userId) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(userId);
    }

    public Mono<Map<String, Boolean>> checkItem(UUID userId, WishlistRequest req) {
        return wishlistRepository.existsByUserIdAndEntityTypeAndEntityId(
                userId, req.getEntityType().name(), req.getEntityId())
                .map(exists -> Map.of("is_favorited", exists));
    }

    public Mono<WishlistItem> addItem(UUID userId, WishlistRequest req) {
        return wishlistRepository.existsByUserIdAndEntityTypeAndEntityId(
                userId, req.getEntityType().name(), req.getEntityId())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new RuntimeException("Déjà dans les favoris"));
                    WishlistItem item = WishlistItem.builder()
                            .userId(userId).entityType(req.getEntityType()).entityId(req.getEntityId())
                            .addedAt(Instant.now()).build();
                    return wishlistRepository.save(item);
                });
    }

    public Mono<Void> removeItem(UUID userId, UUID itemId) {
        return wishlistRepository.deleteByUserIdAndId(userId, itemId);
    }
    
    public Mono<Void> clearWishlist(UUID userId) {
        return wishlistRepository.deleteByUserId(userId);
    }
}