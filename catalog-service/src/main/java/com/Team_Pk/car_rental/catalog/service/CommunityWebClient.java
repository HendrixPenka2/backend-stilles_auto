package com.Team_Pk.car_rental.catalog.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CommunityWebClient {

    private final WebClient webClient;
    
    // Cache simple en mémoire (ConcurrentHashMap pour le multi-threading)
    // Clé : "VEHICLE:uuid" ou "ACCESSORY:uuid"
    // Valeur : RatingDto
    private final Map<String, RatingDto> ratingsCache = new ConcurrentHashMap<>();

    // Le DTO interne pour lire la réponse JSON envoyée par Community-Service
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingDto {
        private Double averageRating;
        private Integer reviewCount;
    }

    // On injecte le builder et on définit l'URL de base (Port 8083 pour Community)
    public CommunityWebClient(WebClient.Builder webClientBuilder, 
                              @Value("${community-service.url:http://localhost:8083/api/v1}") String communityServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(communityServiceUrl).build();
    }

    /**
     * Méthode pour récupérer la note d'une entité (VEHICLE ou ACCESSORY).
     * ASTUCE PRO : Utilisation de onErrorResume pour la RÉSILIENCE.
     * Si community-service est éteint, le catalogue ne plante pas ! Il affiche juste 0.
     * 
     * CACHE : Vérifie d'abord le cache avant d'appeler le service distant
     */
    public Mono<RatingDto> getEntityRating(String entityType, UUID entityId) {
        String cacheKey = buildCacheKey(entityType, entityId);
        
        // Si présent dans le cache, on retourne directement
        RatingDto cached = ratingsCache.get(cacheKey);
        if (cached != null) {
            log.debug("📦 Cache HIT pour {}", cacheKey);
            return Mono.just(cached);
        }
        
        // Sinon, on va chercher via HTTP
        log.debug("🌐 Cache MISS pour {}, appel HTTP...", cacheKey);
        return webClient.get()
                .uri("/ratings/{type}/{id}", entityType, entityId)
                .retrieve()
                .bodyToMono(RatingDto.class)
                .doOnSuccess(rating -> {
                    // On met en cache pour les prochaines requêtes
                    ratingsCache.put(cacheKey, rating);
                    log.debug("💾 Mise en cache de {}: {}⭐ ({} avis)", 
                              cacheKey, rating.getAverageRating(), rating.getReviewCount());
                })
                .onErrorResume(error -> {
                    log.warn("Impossible de joindre Community-Service pour {} {}, fallback à 0.", entityType, entityId);
                    return Mono.just(new RatingDto(0.0, 0));
                });
    }
    
    /**
     * Invalide le cache pour une entité spécifique
     * Appelé par ReviewEventListener quand un avis est approuvé/rejeté
     */
    public void invalidateCache(String entityType, UUID entityId) {
        String cacheKey = buildCacheKey(entityType, entityId);
        RatingDto removed = ratingsCache.remove(cacheKey);
        if (removed != null) {
            log.info("🗑️ Cache invalidé pour {} (ancienne note: {}⭐)", 
                     cacheKey, removed.getAverageRating());
        } else {
            log.debug("ℹ️ Aucune entrée en cache pour {}", cacheKey);
        }
    }
    
    /**
     * Construit la clé de cache unique : "VEHICLE:uuid" ou "ACCESSORY:uuid"
     */
    private String buildCacheKey(String entityType, UUID entityId) {
        return entityType.toUpperCase() + ":" + entityId.toString();
    }
}
