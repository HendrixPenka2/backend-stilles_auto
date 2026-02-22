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
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleImageService vehicleImageService;
    // ==========================================
    // ROUTES PUBLIQUES (Ouvertes à tous)
    // ==========================================

    @GetMapping("/vehicles")
    // CORRECTION 1 : @ModelAttribute dit à Swagger d'éclater l'objet en "Query Params" individuels
    public Mono<PaginatedResponse<Vehicle>> getVehicles(@ParameterObject @ModelAttribute VehicleSearchCriteria criteria) {
        return vehicleService.searchVehicles(criteria);
    }

    @GetMapping("/vehicles/{id}")
    public Mono<VehicleDetailResponse> getVehicleById(@PathVariable("id") UUID id) {
        return vehicleService.getVehicleById(id);
    }

    // --- DANS LA SECTION ROUTES PUBLIQUES ---

    @GetMapping("/vehicles/featured")
    public Mono<List<Vehicle>> getFeaturedVehicles(@RequestParam(name = "limit", required = false) Integer limit) {
        return vehicleService.getFeaturedVehicles(limit);
    }

    //recuperer toutes les options de filtres 
    @GetMapping("/vehicles/filters/options")
    public Mono<FilterOptionsResponse> getFilterOptions() {
        return vehicleService.getFilterOptions();
    }

    @GetMapping("/vehicles/{id}/availability")
    public Mono<VehicleCalendarResponse> getAvailability(
            @PathVariable("id") UUID id,
            @RequestParam(name = "from", required = false) LocalDate from, // Ajoute 'name'
            @RequestParam(name = "to", required = false) LocalDate to) {   // Ajoute 'name'
        return vehicleService.getVehicleAvailability(id, from, to);
    }


    // ==========================================
    // ROUTES ADMIN (Protégées par SecurityConfig)
    // ==========================================

    @PostMapping("/admin/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> createVehicle(@RequestBody VehicleRequest request, Authentication authentication) {
        // On récupère l'UUID de l'admin connecté depuis le JWT
        UUID adminId = UUID.fromString(authentication.getName());
        return vehicleService.createVehicle(request, adminId);
    }

    @PatchMapping("/admin/vehicles/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public Mono<Vehicle> updateVehicle(
                @PathVariable("id") UUID id, 
                @RequestBody VehicleRequest request, 
                Authentication auth) { // <-- L'objet Authentication est ajouté ici
                
            // Extraction de l'ID Admin depuis le JWT
            UUID adminId = UUID.fromString(auth.getName());
            return vehicleService.updateVehicle(id, request, adminId);
        }

    @DeleteMapping("/admin/vehicles/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public Mono<Void> deleteVehicle(@PathVariable("id") UUID id, Authentication auth) {
            UUID adminId = UUID.fromString(auth.getName());
            return vehicleService.deleteVehicle(id, adminId); // On passe l'adminId
        }

    @PatchMapping("/admin/vehicles/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> reactivateVehicle(@PathVariable("id") UUID id, Authentication auth) {
        return vehicleService.reactivateVehicle(id);
    }

        // ==========================================
    // ROUTES IMAGES (ADMIN)
    // ==========================================

    @PostMapping(value = "/admin/vehicles/{id}/images", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<VehicleImage> uploadVehicleImages(
            @PathVariable("id") UUID id,
            @RequestPart("files") Flux<FilePart> files) {
        // @RequestPart("files") correspond au nom du champ dans le form-data (files[])
        return vehicleImageService.uploadImages(id, files);
    }

    // --- DANS LA SECTION ROUTES ADMIN ---

    @GetMapping("/admin/vehicles")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<PaginatedResponse<Vehicle>> getAdminVehicles(@ParameterObject @ModelAttribute VehicleSearchCriteria criteria) {
        return vehicleService.getAdminVehicles(criteria);
    }

    @DeleteMapping("/admin/vehicles/{id}/images/{image_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteVehicleImage(
            @PathVariable("id") UUID vehicleId,
            @PathVariable("image_id") UUID imageId) {
        return vehicleImageService.deleteImage(vehicleId, imageId);
    }

        // --- ROUTES ADMIN ---
    @PostMapping("/admin/vehicles/{id}/availability/block")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UUID> blockPeriod(@PathVariable("id") UUID id, @RequestBody BlockPeriodRequest request) {
        return vehicleService.blockVehiclePeriod(id, request);
    }

    @DeleteMapping("/admin/vehicles/{id}/availability/{block_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> unblockPeriod(
            @PathVariable("id") UUID vehicleId, 
            @PathVariable("block_id") UUID blockId) {
        return vehicleService.unblockVehiclePeriod(vehicleId, blockId);
    }

    @PatchMapping("/admin/vehicles/{id}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> reorderVehicleImages(
            @PathVariable("id") UUID vehicleId,
            @RequestBody ImageReorderRequest request) {
        return vehicleImageService.reorderImages(vehicleId, request);
    }

    // @GetMapping("/admin/vehicles/{id}/stock-history")
    // @PreAuthorize("hasRole('ADMIN')")
    // public Flux<StockMovement> getVehicleStockHistory(@PathVariable("id") UUID id) {
    //     return vehicleService.getVehicleStockHistory(id);
    // }

    // ==========================================
    // GESTION DU STOCK ADMIN (VÉHICULES)
    // ==========================================

    @PatchMapping("/admin/vehicles/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockAdjustmentResponse> adjustVehicleStock(
            @PathVariable("id") UUID id, 
            @RequestBody StockAdjustmentRequest req, 
            Authentication auth) {
        // On récupère l'ID de l'admin
        UUID adminId = UUID.fromString(auth.getName());
        return vehicleService.adjustVehicleStock(id, req, adminId);
    }

    @GetMapping("/admin/vehicles/{id}/stock-history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockHistoryListResponse> getStockHistory(@PathVariable("id") UUID id) {
        return vehicleService.getVehicleStockHistory(id);
    }

}