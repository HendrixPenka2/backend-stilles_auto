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

import com.Team_Pk.car_rental.community.dto.RatingStats;
import com.Team_Pk.car_rental.community.dto.ReviewRequest;
import com.Team_Pk.car_rental.community.dto.ReviewStatsResponse;
import com.Team_Pk.car_rental.community.entity.Review;
import com.Team_Pk.car_rental.community.entity.enums.ReviewStatus;
import com.Team_Pk.car_rental.community.repository.ReviewRepository;
import com.Team_Pk.car_rental.community.service.ReviewService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Gestion des avis clients sur les véhicules et accessoires")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;

    /**
     * 🔗 ENDPOINT INTER-SERVICES - Récupère les statistiques de notation d'une entité (véhicule ou accessoire)
     * Utilisé par catalog-service via CommunityWebClient pour afficher les notes moyennes
     * Accessible sans authentification pour permettre l'affichage public des notes
     */
    @Operation(summary = "Obtenir les statistiques de notation", 
               description = "Retourne la note moyenne et le nombre total d'avis pour un véhicule ou accessoire")
    @GetMapping("/ratings/{entityType}/{entityId}")
    public Mono<RatingStats> getEntityRatings(
            @Parameter(description = "Type d'entité : VEHICLE ou ACCESSORY", example = "VEHICLE")
            @PathVariable("entityType") String entityType,
            @Parameter(description = "Identifiant UUID de l'entité", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("entityId") UUID entityId) {
        return reviewRepository.getStatsFromView(entityType.toUpperCase(), entityId)
                .defaultIfEmpty(new RatingStats(0.0, 0)); 
    }

    /**
     * 📋 ENDPOINT PUBLIC - Liste tous les avis approuvés (APPROVED) d'une entité spécifique
     * Permet aux clients de consulter les avis avant de louer un véhicule ou acheter un accessoire
     * Accessible sans authentification
     */
    @Operation(summary = "Lister les avis publics", 
               description = "Récupère tous les avis approuvés pour un véhicule ou accessoire donné")
    @GetMapping("/reviews")
    public Flux<Review> getPublicReviews(
            @Parameter(description = "Type d'entité : VEHICLE ou ACCESSORY", example = "VEHICLE")
            @RequestParam("entityType") String entityType,
            @Parameter(description = "Identifiant UUID de l'entité", example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam("entityId") UUID entityId) {
        return reviewService.getPublicReviews(entityType, entityId);
    }

    /**
     * ✍️ ENDPOINT CLIENT - Soumettre un nouvel avis sur un véhicule ou accessoire
     * Nécessite d'être authentifié - L'avis est créé avec le statut PENDING (en attente de modération)
     * Envoie un événement RabbitMQ "review.submitted" pour notifier les admins
     */
    @Operation(summary = "Soumettre un avis", 
               description = "Créer un nouvel avis (note + commentaire). Statut initial : PENDING")
    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public Mono<Review> submitReview(
            @Parameter(description = "Données de l'avis : entityType, entityId, rating (1-5), comment")
            @Valid @RequestBody ReviewRequest req, 
            Authentication auth) {
        return reviewService.submitReview(req, UUID.fromString(auth.getName()));
    }

    /**
     * 👤 ENDPOINT CLIENT - Récupérer tous mes avis personnels
     * Affiche tous les avis de l'utilisateur connecté (peu importe le statut : PENDING, APPROVED, REJECTED)
     * Utile pour que le client voie l'historique de ses contributions
     */
    @Operation(summary = "Mes avis", 
               description = "Liste tous les avis soumis par l'utilisateur authentifié")
    @GetMapping("/reviews/my")
    @PreAuthorize("isAuthenticated()")
    public Flux<Review> getMyReviews(Authentication auth) {
        return reviewService.getMyReviews(UUID.fromString(auth.getName()));
    }

    /**
     * ✏️ ENDPOINT CLIENT - Modifier un de mes avis existants
     * Permet de changer la note ou le commentaire - L'utilisateur ne peut modifier que ses propres avis
     * Le statut repasse automatiquement à PENDING pour une nouvelle modération
     */
    @Operation(summary = "Modifier mon avis", 
               description = "Met à jour un avis existant. L'utilisateur doit en être l'auteur")
    @PatchMapping("/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public Mono<Review> updateReview(
            @Parameter(description = "Identifiant UUID de l'avis à modifier", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouvelles données de l'avis")
            @Valid @RequestBody ReviewRequest req, 
            Authentication auth) {
        return reviewService.updateReview(id, req, UUID.fromString(auth.getName()));
    }

    /**
     * 🗑️ ENDPOINT CLIENT - Supprimer un de mes avis
     * L'utilisateur peut supprimer ses propres avis uniquement
     * Suppression définitive de la base de données
     */
    @Operation(summary = "Supprimer mon avis", 
               description = "Supprime définitivement un avis. L'utilisateur doit en être l'auteur")
    @DeleteMapping("/reviews/{id}")
    @PreAuthorize("isAuthenticated()")
    public Mono<Void> deleteReview(
            @Parameter(description = "Identifiant UUID de l'avis à supprimer", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            Authentication auth) {
        return reviewService.deleteReview(id, UUID.fromString(auth.getName()));
    }

    /**
     * 👨‍💼 ENDPOINT ADMIN - Lister TOUS les avis (modération)
     * Permet aux admins de voir tous les avis sans filtre, ou filtrer par statut (PENDING, APPROVED, REJECTED)
     * Utilisé pour le tableau de bord de modération
     */
    @Operation(summary = "[ADMIN] Lister tous les avis", 
               description = "Récupère tous les avis du système avec filtre optionnel par statut")
    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Review> getAllReviews(
            @Parameter(description = "Filtrer par statut : PENDING, APPROVED, REJECTED (optionnel)", example = "PENDING")
            @RequestParam(value = "status", required = false) String status) {
        return reviewService.getAllReviews(status);
    }

    /**
     * 📊 ENDPOINT ADMIN - Statistiques globales des avis
     * Retourne le nombre total d'avis par statut (pending, approved, rejected) et la note moyenne globale
     * Utilisé pour les dashboards administrateurs
     */
    @Operation(summary = "[ADMIN] Statistiques des avis", 
               description = "Nombre d'avis par statut et note moyenne globale du système")
    @GetMapping("/admin/reviews/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ReviewStatsResponse> getStats() {
        return reviewService.getStats();
    }

    /**
     * ✅❌ ENDPOINT ADMIN - Modérer un avis (approuver ou rejeter)
     * Change le statut d'un avis PENDING vers APPROVED (publié) ou REJECTED (refusé)
     * Envoie des événements RabbitMQ "review.approved" ou "review.rejected" pour notifier l'auteur
     * L'admin peut ajouter une note explicative (surtout en cas de rejet)
     */
    @Operation(summary = "[ADMIN] Modérer un avis", 
               description = "Approuver ou rejeter un avis en attente. Envoie une notification à l'auteur")
    @PatchMapping("/admin/reviews/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Review> moderateReview(
            @Parameter(description = "Identifiant UUID de l'avis à modérer", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id,
            @Parameter(description = "Nouveau statut : APPROVED ou REJECTED", example = "APPROVED")
            @RequestParam("status") ReviewStatus status,
            @Parameter(description = "Note de l'admin expliquant la décision (optionnel)", example = "Contenu inapproprié")
            @RequestParam(value = "adminNote", required = false) String adminNote,
            Authentication auth) {
        return reviewService.moderateReview(id, status, adminNote, UUID.fromString(auth.getName()));
    }
}
