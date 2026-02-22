package com.Team_Pk.car_rental.catalog.controller;

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
import com.Team_Pk.car_rental.catalog.entity.StockMovement;
import com.Team_Pk.car_rental.catalog.service.AccessoryImageService;
import com.Team_Pk.car_rental.catalog.service.AccessoryService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AccessoryController {

    private final AccessoryService accessoryService;
    private final AccessoryImageService imageService;

    // --- PUBLIC ---
    @GetMapping("/accessories")
    public Mono<PaginatedResponse<Accessory>> getAccessories(@ParameterObject @ModelAttribute AccessorySearchCriteria criteria) {
        return accessoryService.searchAccessories(criteria);
    }

    @GetMapping("/accessories/{id}")
    public Mono<AccessoryDetailResponse> getById(@PathVariable("id") UUID id) {
        return accessoryService.getAccessoryById(id);
    }

    @GetMapping("/accessories/featured")
    public Mono<List<Accessory>> getFeatured() {
        return accessoryService.getFeatured();
    }

    @GetMapping("/accessories/filters/options")
    public Mono<AccessoryFilterOptions> getOptions() {
        return accessoryService.getFilterOptions();
    }

    // --- ADMIN CRUD ---
    @PostMapping("/admin/accessories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Accessory> create(@RequestBody AccessoryRequest req, Authentication auth) {
        return accessoryService.createAccessory(req, UUID.fromString(auth.getName()));
    }

    @PatchMapping("/admin/accessories/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public Mono<Accessory> update(
                @PathVariable("id") UUID id, 
                @RequestBody AccessoryRequest req, 
                Authentication auth) { // <-- L'objet Authentication est ajouté ici
                
            // Extraction de l'ID Admin depuis le JWT
            UUID adminId = UUID.fromString(auth.getName());
            return accessoryService.updateAccessory(id, req, adminId);
        }
    @DeleteMapping("/admin/accessories/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public Mono<Void> delete(@PathVariable("id") UUID id, Authentication auth) {
            UUID adminId = UUID.fromString(auth.getName());
            return accessoryService.deleteAccessory(id, adminId);
        }

    // --- ADMIN IMAGES ---
    @PostMapping(value = "/admin/accessories/{id}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AccessoryImage> uploadImages(@PathVariable("id") UUID id, @RequestPart("files") Flux<FilePart> files) {
        return imageService.uploadImages(id, files);
    }

    @PatchMapping("/admin/accessories/{id}/images/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> reorderImages(@PathVariable("id") UUID id, @RequestBody ImageReorderRequest req) {
        return imageService.reorderImages(id, req);
    }

    @DeleteMapping("/admin/accessories/{id}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteImage(@PathVariable("id") UUID id, @PathVariable("imageId") UUID imageId) {
        return imageService.deleteImage(id, imageId);
    }

    // --- ADMIN STOCK ---
    @PatchMapping("/admin/accessories/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockAdjustmentResponse> adjustStock(@PathVariable("id") UUID id, @RequestBody StockAdjustmentRequest req, Authentication auth) {
        return accessoryService.adjustStock(id, req, UUID.fromString(auth.getName()));
    }

    @GetMapping("/admin/accessories/{id}/stock-history")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<StockHistoryListResponse> getHistory(@PathVariable("id") UUID id) { // <-- Changé ici (Mono au lieu de Flux)
        return accessoryService.getStockHistory(id);
    }
    
    // Pour le dashboard (liste admin avec tous les statuts)
    @GetMapping("/admin/accessories")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<PaginatedResponse<Accessory>> getAdminAccessories(@ParameterObject @ModelAttribute AccessorySearchCriteria criteria) {
        // En vrai projet, on enlève le filtre "is_active=true" ici, mais pour l'instant on réutilise
        return accessoryService.searchAccessories(criteria);
    }
}