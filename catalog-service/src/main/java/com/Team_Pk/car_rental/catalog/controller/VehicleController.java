package com.Team_Pk.car_rental.catalog.controller;

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

    @DeleteMapping("/admin/vehicles/{id}/images/{image_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteVehicleImage(
            @PathVariable("id") UUID vehicleId,
            @PathVariable("image_id") UUID imageId) {
        return vehicleImageService.deleteImage(vehicleId, imageId);
    }

}