package com.Team_Pk.car_rental.community.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.Team_Pk.car_rental.community.dto.WishlistRequest;
import com.Team_Pk.car_rental.community.entity.WishlistItem;
import com.Team_Pk.car_rental.community.entity.enums.WishlistEntityType;
import com.Team_Pk.car_rental.community.service.WishlistService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // Toute la Wishlist nécessite d'être connecté
@Tag(name = "Wishlist", description = "Gestion de la liste de favoris (véhicules et accessoires)")
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 👤 ENDPOINT CLIENT - Récupérer ma liste de favoris complète
     * Retourne tous les véhicules et accessoires que l'utilisateur a ajoutés dans ses favoris
     * Utilisé pour afficher la page "Mes Favoris" dans le frontend
     */
    @Operation(summary = "Ma liste de favoris", 
               description = "Liste tous les articles (véhicules et accessoires) dans ma wishlist")
    @GetMapping
    public Flux<WishlistItem> getMyWishlist(Authentication auth) {
        return wishlistService.getMyWishlist(UUID.fromString(auth.getName()));
    }

/**
 * ✔ ENDPOINT CLIENT - Vérifier si un article est dans mes favoris
 * Retourne {"inWishlist": true/false} pour afficher l'icône cœur rempli ou vide dans le frontend
 * Utilisé sur les pages de détail de véhicules/accessoires
 */
    @Operation(summary = "Vérifier si dans ma wishlist", 
            description = "Indique si un véhicule ou accessoire est déjà dans mes favoris")
    @GetMapping("/check")
    public Mono<Map<String, Boolean>> checkItem(
            @Parameter(description = "Type d'entité (VEHICLE ou ACCESSORY)", required = true, example = "VEHICLE")
            @RequestParam("entityType") String entityType,
            @Parameter(description = "ID de l'entité", required = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @RequestParam("entityId") UUID entityId,
            Authentication auth) {
        
        WishlistRequest req = new WishlistRequest();
        req.setEntityType(WishlistEntityType.valueOf(entityType.toUpperCase()));
        req.setEntityId(entityId);
        
        return wishlistService.checkItem(UUID.fromString(auth.getName()), req);
    }

    /**
     * ➕ ENDPOINT CLIENT - Ajouter un article dans mes favoris
     * Crée un nouveau favori (véhicule ou accessoire) si pas déjà présent
     * Action déclenchée quand l'utilisateur clique sur l'icône cœur vide
     */
    @Operation(summary = "Ajouter aux favoris", 
               description = "Ajoute un véhicule ou accessoire à ma liste de favoris")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WishlistItem> addItem(
            @Parameter(description = "Données de l'article : entityType (VEHICLE/ACCESSORY) et entityId (UUID)")
            @Valid @RequestBody WishlistRequest req, 
            Authentication auth) {
        return wishlistService.addItem(UUID.fromString(auth.getName()), req);
    }

    /**
     * ➖ ENDPOINT CLIENT - Retirer un article de mes favoris
     * Supprime un favori spécifique par son ID
     * Action déclenchée quand l'utilisateur clique sur l'icône cœur plein ou sur "Retirer" dans la liste
     */
    @Operation(summary = "Retirer des favoris", 
               description = "Supprime un article spécifique de ma liste de favoris")
    @DeleteMapping("/{id}")
    public Mono<Void> removeItem(
            @Parameter(description = "Identifiant UUID de l'élément wishlist à retirer", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            Authentication auth) {
        return wishlistService.removeItem(UUID.fromString(auth.getName()), id);
    }

    /**
     * 🗑️ ENDPOINT CLIENT - Vider complètement ma liste de favoris
     * Supprime tous les favoris de l'utilisateur connecté en une seule opération
     * Action déclenchée par le bouton "Vider la wishlist" dans le frontend
     */
    @Operation(summary = "Vider ma wishlist", 
               description = "Supprime tous les articles de ma liste de favoris")
    @DeleteMapping
    public Mono<Void> clearWishlist(Authentication auth) {
        return wishlistService.clearWishlist(UUID.fromString(auth.getName()));
    }
}