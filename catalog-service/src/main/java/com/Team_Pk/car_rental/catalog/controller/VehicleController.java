package com.Team_Pk.car_rental.catalog.controller;

import com.Team_Pk.car_rental.catalog.dto.AvailabilityResponse;
import com.Team_Pk.car_rental.catalog.dto.BlockPeriodRequest;
import com.Team_Pk.car_rental.catalog.dto.FilterOptionsResponse;
import com.Team_Pk.car_rental.catalog.dto.ImageReorderRequest;
import com.Team_Pk.car_rental.catalog.dto.PaginatedResponse;
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

    // --- ROUTE PUBLIQUE (Pour le calendrier côté Front-end) ---
    @GetMapping("/vehicles/{id}/availability")
    public Flux<AvailabilityResponse> getAvailability(@PathVariable("id") UUID id) {
        return vehicleService.getVehicleAvailability(id);
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

    @PatchMapping("/admin/vehicles/{id}") // Utilisation de l'annotation Spring
    @PreAuthorize("hasRole('ADMIN')")
    // CORRECTION 2 : Préciser ("id") dans le PathVariable
    public Mono<Vehicle> updateVehicle(@PathVariable("id") UUID id, @RequestBody VehicleRequest request) {
        return vehicleService.updateVehicle(id, request);
    }

    @DeleteMapping("/admin/vehicles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    // CORRECTION 2 : Préciser ("id") dans le PathVariable
    public Mono<Void> deleteVehicle(@PathVariable("id") UUID id) {
        return vehicleService.deleteVehicle(id);
    }

    @PatchMapping("/admin/vehicles/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Vehicle> reactivateVehicle(@PathVariable("id") UUID id) {
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

}