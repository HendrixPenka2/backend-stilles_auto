package com.Team_Pk.car_rental.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Team_Pk.car_rental.catalog.dto.CatalogStatsResponse;
import com.Team_Pk.car_rental.catalog.service.CatalogStatsService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/admin/catalog")
@RequiredArgsConstructor
@Tag(name = "Statistiques Catalogue", description = "Dashboard administrateur - Statistiques globales du catalogue")
public class CatalogAdminController {

    private final CatalogStatsService statsService;

    /**
     * 📊 ENDPOINT ADMIN - Statistiques globales du catalogue
     * Retourne : 
     * - Nombre total de véhicules (actifs/inactifs)
     * - Nombre total d'accessoires (actifs/inactifs)
     * - Valeur totale du stock
     * - Statistiques par catégorie
     * Utilisé pour le dashboard administrateur
     */
    @Operation(summary = "[ADMIN] Statistiques du catalogue", 
               description = "Tableau de bord : totaux véhicules, accessoires, valeur stock")
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<CatalogStatsResponse> getCatalogStats() {
        return statsService.getDashboardStats();
    }
}