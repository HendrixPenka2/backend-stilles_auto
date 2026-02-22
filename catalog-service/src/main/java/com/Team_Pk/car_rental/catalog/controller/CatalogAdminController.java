package com.Team_Pk.car_rental.catalog.controller;

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
public class CatalogAdminController {

    private final CatalogStatsService statsService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<CatalogStatsResponse> getCatalogStats() {
        return statsService.getDashboardStats();
    }
}