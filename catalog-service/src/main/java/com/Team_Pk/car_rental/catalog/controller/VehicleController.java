package com.Team_Pk.car_rental.catalog.controller;

import com.Team_Pk.car_rental.catalog.dto.BlockPeriodRequest;
import com.Team_Pk.car_rental.catalog.dto.FilterOptionsResponse;
import com.Team_Pk.car_rental.catalog.dto.ImageReorderRequest;
import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentRequest;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentResponse;
import com.Team_Pk.car_rental.catalog.dto.StockHistoryListResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleCalendarResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleDetailResponse;
import com.Team_Pk.car_rental.catalog.dto.VehicleRequest;
import com.Team_Pk.car_rental.catalog.dto.VehicleSearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import com.Team_Pk.car_rental.catalog.service.VehicleImageService;
import com.Team_Pk.car_rental.catalog.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Véhicules", description = "Gestion du catalogue de véhicules : recherche, détails, disponibilités et administration")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleImageService vehicleImageService;
    
    // ==========================================
    // ROUTES PUBLIQUES (Accessibles à tous les visiteurs)
    // ==========================================

    /**
     * 🔍 ENDPOINT PUBLIC - Recherche de véhicules avec filtres et pagination
     * Filtres disponibles : catégorie, marque, modèle, année, transmission, carburant, prix
     * Supporte le tri (tri par prix, année, création)
     * Retourne une page de résultats avec métadonnées (total, page, taille)
     */
    @Operation(summary = "Rechercher des véhicules", 
               description = "Liste paginée avec filtres multiples : catégorie, marque, transmission, prix, etc.")
    @GetMapping("/vehicles")
    public Mono<PaginatedResponse<Vehicle>> getVehicles(
            @ParameterObject @ModelAttribute VehicleSearchCriteria criteria) {
        return vehicleService.searchVehicles(criteria);
    }

    /**
     * 🚗 ENDPOINT PUBLIC - Détails complets d'un véhicule spécifique
     * Retourne : informations du véhicule + liste des images + note moyenne + nombre d'avis
     * Appelle le Community Service (CommunityWebClient) pour récupérer les statistiques d'avis
     * Utilisé pour afficher la page de détail d'un véhicule
     */
    @Operation(summary = "Détails d'un véhicule", 
               description = "Informations complètes : caractéristiques, images, note moyenne et nombre d'avis")
    @GetMapping("/vehicles/{id}")
    public Mono<VehicleDetailResponse> getVehicleById(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id) {
        return vehicleService.getVehicleById(id);
    }

    /**
     * ⭐ ENDPOINT PUBLIC - Liste des véhicules mis en avant
     * Retourne les véhicules avec is_featured=true
     * Utilisé pour afficher la section "Véhicules populaires" ou "Recommandés" sur la page d'accueil
     * Limite : par défaut 6 véhicules (paramètre optionnel)
     */
    @Operation(summary = "Véhicules en vedette", 
               description = "Liste des véhicules populaires ou recommandés pour la page d'accueil")
    @GetMapping("/vehicles/featured")
    public Mono<List<Vehicle>> getFeaturedVehicles(
            @Parameter(description = "Nombre maximum de véhicules à retourner (par défaut : 6)", example = "6")
            @RequestParam(name = "limit", required = false) Integer limit) {
        return vehicleService.getFeaturedVehicles(limit);
    }

    /**
     * 🎨 ENDPOINT PUBLIC - Options de filtres disponibles pour les véhicules
     * Retourne la liste des valeurs possibles : toutes les marques, modèles, catégories, années, etc.
     * Utilisé pour peupler les listes déroulantes de filtres dans le frontend
     * Permet une recherche dynamique basée sur les données réelles du catalogue
     */
    @Operation(summary = "Options de filtres", 
               description = "Liste toutes les valeurs disponibles pour les filtres (marques, modèles, catégories...)")
    @GetMapping("/vehicles/filters/options")
    public Mono<FilterOptionsResponse> getFilterOptions() {
        return vehicleService.getFilterOptions();
    }

    /**
     * 📅 ENDPOINT PUBLIC - Calendrier de disponibilité d'un véhicule
     * Affiche les périodes bloquées (réservations ou blocages administratifs)
     * Permet aux clients de voir les dates disponibles pour la location
     * Paramètres optionnels : from/to pour limiter la période affichée
     */
    @Operation(summary = "Calendrier de disponibilité", 
               description = "Affiche les périodes bloquées/réservées pour planifier une location")
    @GetMapping("/vehicles/{id}/availability")
    public Mono<VehicleCalendarResponse> getAvailability(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id,
            @Parameter(description = "Date de début (optionnel)", example = "2026-03-01")
            @RequestParam(name = "from", required = false) LocalDate from,
            @Parameter(description = "Date de fin (optionnel)", example = "2026-03-31")
            @RequestParam(name = "to", required = false) LocalDate to) {
        return vehicleService.getVehicleAvailability(id, from, to);
    }


    // ==========================================
    // ROUTES ADMIN (Réservées aux administrateurs)
    // ==========================================

    /**
     * ➕ ENDPOINT ADMIN - Créer un nouveau véhicule dans le catalogue
     * Enregistre toutes les caractéristiques : marque, modèle, catégorie, prix, stock, etc.
     * Stocke l'ID de l'admin qui a créé le véhicule (traçabilité)
     * Le véhicule est créé avec is_active=true par défaut
     */
    @Operation(summary = "[ADMIN] Créer un véhicule", 
               description = "Ajoute un nouveau véhicule au catalogue avec toutes ses caractéristiques")
    @PostMapping("/admin/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> createVehicle(
            @Parameter(description = "Données complètes du véhicule : marque, modèle, année, prix, stock...")
            @RequestBody VehicleRequest request, 
            Authentication authentication) {
        UUID adminId = UUID.fromString(authentication.getName());
        return vehicleService.createVehicle(request, adminId);
    }

    /**
     * ✏️ ENDPOINT ADMIN - Modifier un véhicule existant
     * Permet de mettre à jour toutes les informations sauf l'ID
     * Stocke l'ID de l'admin qui a modifié (updated_by)
     * Met à jour automatiquement le champ updated_at
     */
    @Operation(summary = "[ADMIN] Modifier un véhicule", 
               description = "Met à jour les informations d'un véhicule existant")
    @PatchMapping("/admin/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> updateVehicle(
            @Parameter(description = "Identifiant UUID du véhicule à modifier", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouvelles données du véhicule")
            @RequestBody VehicleRequest request, 
            Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return vehicleService.updateVehicle(id, request, adminId);
    }

    /**
     * 🗑️ ENDPOINT ADMIN - Supprimer un véhicule (soft delete)
     * Ne supprime PAS physiquement de la base de données
     * Passe is_active=false et deleted_at=now()
     * Le véhicule n'apparaît plus dans les recherches publiques
     */
    @Operation(summary = "[ADMIN] Supprimer un véhicule", 
               description = "Désactivation logique (soft delete) - le véhicule devient invisible")
    @DeleteMapping("/admin/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteVehicle(
            @Parameter(description = "Identifiant UUID du véhicule à supprimer", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return vehicleService.deleteVehicle(id, adminId);
    }

    /**
     * ♻️ ENDPOINT ADMIN - Réactiver un véhicule supprimé
     * Annule le soft delete : is_active=true, deleted_at=null
     * Le véhicule redevient visible dans les recherches publiques
     * Utile pour corriger une suppression accidentelle
     */
    @Operation(summary = "[ADMIN] Réactiver un véhicule", 
               description = "Annule la suppression et rend le véhicule à nouveau visible")
    @PatchMapping("/admin/vehicles/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> reactivateVehicle(
            @Parameter(description = "Identifiant UUID du véhicule à réactiver", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            Authentication auth) {
        return vehicleService.reactivateVehicle(id);
    }

    // ==========================================
    // GESTION DES IMAGES (ADMIN uniquement)
    // ==========================================

    /**
     * 📷 ENDPOINT ADMIN - Télécharger des images pour un véhicule
     * Accepte plusieurs fichiers en multipart/form-data
     * Stocke les fichiers et crée les enregistrements dans vehicle_images
     * Ordre d'affichage : déterminé par display_order (modifiable via reorder)
     */
    @Operation(summary = "[ADMIN] Télécharger des images", 
               description = "Ajoute une ou plusieurs images au véhicule (multipart/form-data)")
    @PostMapping(value = "/admin/vehicles/{id}/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<VehicleImage> uploadVehicleImages(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id,
            @Parameter(description = "Fichiers images à uploader (champ form-data : 'files')")
            @RequestPart("files") Flux<FilePart> files) {
        return vehicleImageService.uploadImages(id, files);
    }

    /**
     * 🗑️ ENDPOINT ADMIN - Supprimer une image spécifique d'un véhicule
     * Supprime le fichier physique ET l'enregistrement en base
     * Réorganise automatiquement l'ordre (display_order) des images restantes
     */
    @Operation(summary = "[ADMIN] Supprimer une image", 
               description = "Supprime une image spécifique du véhicule")
    @DeleteMapping("/admin/vehicles/{id}/images/{image_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteVehicleImage(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID vehicleId,
            @Parameter(description = "Identifiant UUID de l'image à supprimer", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable("image_id") UUID imageId) {
        return vehicleImageService.deleteImage(vehicleId, imageId);
    }

    /**
     * 🔄 ENDPOINT ADMIN - Réorganiser l'ordre d'affichage des images
     * Modifie display_order pour changer l'ordre des images dans le carousel
     * Utile pour mettre la meilleure photo en premier
     */
    @Operation(summary = "[ADMIN] Réorganiser les images", 
               description = "Change l'ordre d'affichage des images du véhicule")
    @PatchMapping("/admin/vehicles/{id}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> reorderVehicleImages(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID vehicleId,
            @Parameter(description = "Nouvel ordre : liste d'IDs d'images dans l'ordre souhaité")
            @RequestBody ImageReorderRequest request) {
        return vehicleImageService.reorderImages(vehicleId, request);
    }

    /**
     * 👨‍💼 ENDPOINT ADMIN - Liste des véhicules pour le dashboard administrateur
     * Affiche TOUS les véhicules (même ceux supprimés avec is_active=false)
     * Permet aux admins de gérer l'ensemble du catalogue
     */
    @Operation(summary = "[ADMIN] Liste admin des véhicules", 
               description = "Liste complète incluant les véhicules supprimés/inactifs")
    @GetMapping("/admin/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<PaginatedResponse<Vehicle>> getAdminVehicles(
            @ParameterObject @ModelAttribute VehicleSearchCriteria criteria) {
        return vehicleService.getAdminVehicles(criteria);
    }

    // ==========================================
    // GESTION DES DISPONIBILITÉS (ADMIN)
    // ==========================================

    /**
     * 🚫 ENDPOINT ADMIN - Bloquer une période de disponibilité
     * Empêche la location du véhicule sur une plage de dates
     * Utilisé pour la maintenance, réparations ou indisponibilité programmée
     * Raison obligatoire pour traçabilité
     */
    @Operation(summary = "[ADMIN] Bloquer une période", 
               description = "Rend le véhicule indisponible pour une période donnée (maintenance, etc.)")
    @PostMapping("/admin/vehicles/{id}/availability/block")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UUID> blockPeriod(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Période à bloquer : from, to, reason")
            @RequestBody BlockPeriodRequest request) {
        return vehicleService.blockVehiclePeriod(id, request);
    }

    /**
     * ✅ ENDPOINT ADMIN - Débloquer une période précédemment bloquée
     * Supprime le blocage et rend à nouveau le véhicule disponible
     * Utilisé quand la maintenance est terminée plus tôt que prévu
     */
    @Operation(summary = "[ADMIN] Débloquer une période", 
               description = "Supprime un blocage et restaure la disponibilité du véhicule")
    @DeleteMapping("/admin/vehicles/{id}/availability/{block_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> unblockPeriod(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID vehicleId, 
            @Parameter(description = "Identifiant UUID du blocage à supprimer", example = "123e4567-e89b-12d3-a456-426614174002")
            @PathVariable("block_id") UUID blockId) {
        return vehicleService.unblockVehiclePeriod(vehicleId, blockId);
    }

    // ==========================================
    // GESTION DU STOCK (ADMIN)
    // ==========================================

    /**
     * 📦 ENDPOINT ADMIN - Ajuster le stock d'un véhicule
     * Opérations possibles : ADDITION (ajout de stock) ou SUBTRACTION (retrait)
     * Enregistre chaque mouvement dans stock_movements pour l'historique
     * Raison obligatoire : achat, vente, casse, transfert, etc.
     */
    @Operation(summary = "[ADMIN] Ajuster le stock", 
               description = "Ajouter ou retirer du stock avec traçabilité (historique des mouvements)")
    @PatchMapping("/admin/vehicles/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockAdjustmentResponse> adjustVehicleStock(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Type (ADDITION/SUBTRACTION), quantité et raison")
            @RequestBody StockAdjustmentRequest req, 
            Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return vehicleService.adjustVehicleStock(id, req, adminId);
    }

    /**
     * 📊 ENDPOINT ADMIN - Historique des mouvements de stock
     * Affiche tous les ajustements effectués sur ce véhicule
     * Permet de traçer qui a ajouté/retiré du stock, quand et pourquoi
     * Utile pour les audits et la comptabilité
     */
    @Operation(summary = "[ADMIN] Historique du stock", 
               description = "Liste tous les mouvements de stock avec dates, quantités et raisons")
    @GetMapping("/admin/vehicles/{id}/stock-history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockHistoryListResponse> getStockHistory(
            @Parameter(description = "Identifiant UUID du véhicule", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id) {
        return vehicleService.getVehicleStockHistory(id);
    }

}