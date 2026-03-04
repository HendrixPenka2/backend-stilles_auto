package com.Team_Pk.car_rental.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.Team_Pk.car_rental.catalog.dto.AccessoryDetailResponse;
import com.Team_Pk.car_rental.catalog.dto.AccessoryFilterOptions;
import com.Team_Pk.car_rental.catalog.dto.AccessoryRequest;
import com.Team_Pk.car_rental.catalog.dto.AccessorySearchCriteria;
import com.Team_Pk.car_rental.catalog.dto.ImageReorderRequest;
import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentRequest;
import com.Team_Pk.car_rental.catalog.dto.StockAdjustmentResponse;
import com.Team_Pk.car_rental.catalog.dto.StockHistoryListResponse;
import com.Team_Pk.car_rental.catalog.entity.Accessory;
import com.Team_Pk.car_rental.catalog.entity.AccessoryImage;
import com.Team_Pk.car_rental.catalog.service.AccessoryImageService;
import com.Team_Pk.car_rental.catalog.service.AccessoryService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Accessoires", description = "Gestion du catalogue d'accessoires : recherche, détails et administration")
public class AccessoryController {

    private final AccessoryService accessoryService;
    private final AccessoryImageService imageService;

    // ==========================================
    // ROUTES PUBLIQUES
    // ==========================================

    /**
     * 🔍 ENDPOINT PUBLIC - Recherche d'accessoires avec filtres et pagination
     * Filtres : catégorie, compatibilité, prix, stock disponible
     * Retourne une page de résultats avec métadonnées
     */
    @Operation(summary = "Rechercher des accessoires", 
               description = "Liste paginée avec filtres : catégorie, compatibilité, prix...")
    @GetMapping("/accessories")
    public Mono<PaginatedResponse<Accessory>> getAccessories(
            @ParameterObject @ModelAttribute AccessorySearchCriteria criteria) {
        return accessoryService.searchAccessories(criteria);
    }

    /**
     * 🔧 ENDPOINT PUBLIC - Détails complets d'un accessoire
     * Retourne : informations + images + note moyenne + nombre d'avis
     * Appelle le Community Service pour les statistiques d'avis
     */
    @Operation(summary = "Détails d'un accessoire", 
               description = "Informations complètes : caractéristiques, images, note et avis")
    @GetMapping("/accessories/{id}")
    public Mono<AccessoryDetailResponse> getById(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id) {
        return accessoryService.getAccessoryById(id);
    }

    /**
     * ⭐ ENDPOINT PUBLIC - Accessoires en vedette
     * Retourne les accessoires avec is_featured=true
     * Utilisé pour la section "Accessoires populaires" sur la page d'accueil
     */
    @Operation(summary = "Accessoires en vedette", 
               description = "Liste des accessoires mis en avant sur la page d'accueil")
    @GetMapping("/accessories/featured")
    public Mono<List<Accessory>> getFeatured() {
        return accessoryService.getFeatured();
    }

    /**
     * 🎨 ENDPOINT PUBLIC - Options de filtres pour les accessoires
     * Retourne les valeurs disponibles : catégories, compatibilités, marques
     * Pour peupler les filtres dynamiques du frontend
     */
    @Operation(summary = "Options de filtres", 
               description = "Liste toutes les valeurs disponibles pour filtrer les accessoires")
    @GetMapping("/accessories/filters/options")
    public Mono<AccessoryFilterOptions> getOptions() {
        return accessoryService.getFilterOptions();
    }

    // ==========================================
    // ROUTES ADMIN - CRUD
    // ==========================================

    /**
     * ➕ ENDPOINT ADMIN - Créer un nouvel accessoire
     * Enregistre toutes les caractéristiques : nom, catégorie, prix, stock, compatibilité
     * Traçabilité : created_by = admin connecté
     */
    @Operation(summary = "[ADMIN] Créer un accessoire", 
               description = "Ajoute un nouvel accessoire au catalogue")
    @PostMapping("/admin/accessories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Accessory> create(
            @Parameter(description = "Données de l'accessoire : nom, catégorie, prix, stock...")
            @RequestBody AccessoryRequest req, 
            Authentication auth) {
        return accessoryService.createAccessory(req, UUID.fromString(auth.getName()));
    }

    /**
     * ✏️ ENDPOINT ADMIN - Modifier un accessoire existant
     * Met à jour toutes les informations sauf l'ID
     * Traçabilité : updated_by = admin connecté, updated_at = now()
     */
    @Operation(summary = "[ADMIN] Modifier un accessoire", 
               description = "Met à jour les informations d'un accessoire")
    @PatchMapping("/admin/accessories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Accessory> update(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouvelles données de l'accessoire")
            @RequestBody AccessoryRequest req, 
            Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return accessoryService.updateAccessory(id, req, adminId);
    }
    /**
     * 🗑️ ENDPOINT ADMIN - Supprimer un accessoire (soft delete)
     * is_active=false, deleted_at=now()
     * L'accessoire n'apparaît plus dans les recherches publiques
     */
    @Operation(summary = "[ADMIN] Supprimer un accessoire", 
               description = "Désactivation logique - l'accessoire devient invisible")
    @DeleteMapping("/admin/accessories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            Authentication auth) {
        UUID adminId = UUID.fromString(auth.getName());
        return accessoryService.deleteAccessory(id, adminId);
    }

    // ==========================================
    // ROUTES ADMIN - IMAGES
    // ==========================================

    /**
     * 📷 ENDPOINT ADMIN - Télécharger des images pour un accessoire
     * Multipart/form-data - champ "files"
     * Stocke les fichiers et crée les enregistrements en base
     */
    @Operation(summary = "[ADMIN] Télécharger des images", 
               description = "Ajoute une ou plusieurs images à l'accessoire")
    @PostMapping(value = "/admin/accessories/{id}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AccessoryImage> uploadImages(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Fichiers images (champ form-data : 'files')")
            @RequestPart("files") Flux<FilePart> files) {
        return imageService.uploadImages(id, files);
    }

    /**
     * 🔄 ENDPOINT ADMIN - Réorganiser l'ordre des images
     * Change display_order pour modifier l'ordre du carousel
     */
    @Operation(summary = "[ADMIN] Réorganiser les images", 
               description = "Change l'ordre d'affichage des images de l'accessoire")
    @PatchMapping("/admin/accessories/{id}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> reorderImages(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Nouvel ordre : liste d'IDs d'images")
            @RequestBody ImageReorderRequest req) {
        return imageService.reorderImages(id, req);
    }

    /**
     * 🗑️ ENDPOINT ADMIN - Supprimer une image spécifique
     * Supprime le fichier et l'enregistrement en base
     */
    @Operation(summary = "[ADMIN] Supprimer une image", 
               description = "Supprime une image spécifique de l'accessoire")
    @DeleteMapping("/admin/accessories/{id}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteImage(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Identifiant UUID de l'image", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable("imageId") UUID imageId) {
        return imageService.deleteImage(id, imageId);
    }

    // ==========================================
    // ROUTES ADMIN - STOCK
    // ==========================================

    /**
     * 📦 ENDPOINT ADMIN - Ajuster le stock d'un accessoire
     * ADDITION ou SUBTRACTION avec raison obligatoire
     * Traçabilité complète dans stock_movements
     */
    @Operation(summary = "[ADMIN] Ajuster le stock", 
               description = "Ajouter ou retirer du stock avec historique")
    @PatchMapping("/admin/accessories/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockAdjustmentResponse> adjustStock(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id, 
            @Parameter(description = "Type, quantité et raison")
            @RequestBody StockAdjustmentRequest req, 
            Authentication auth) {
        return accessoryService.adjustStock(id, req, UUID.fromString(auth.getName()));
    }

    /**
     * 📊 ENDPOINT ADMIN - Historique des mouvements de stock
     * Tous les ajustements avec dates, admins et raisons
     */
    @Operation(summary = "[ADMIN] Historique du stock", 
               description = "Liste tous les mouvements de stock de l'accessoire")
    @GetMapping("/admin/accessories/{id}/stock-history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockHistoryListResponse> getHistory(
            @Parameter(description = "Identifiant UUID de l'accessoire", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") UUID id) {
        return accessoryService.getStockHistory(id);
    }
    
    /**
     * 👨‍💼 ENDPOINT ADMIN - Liste admin des accessoires
     * Affiche TOUS les accessoires (même supprimés)
     * Pour le dashboard administrateur
     */
    @Operation(summary = "[ADMIN] Liste admin des accessoires", 
               description = "Liste complète incluant les accessoires supprimés/inactifs")
    @GetMapping("/admin/accessories")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<PaginatedResponse<Accessory>> getAdminAccessories(
            @ParameterObject @ModelAttribute AccessorySearchCriteria criteria) {
        return accessoryService.searchAccessories(criteria);
    }
}