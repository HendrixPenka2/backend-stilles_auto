package com.Team_Pk.car_rental.catalog.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleSearchCriteria {
    private String brand;
    private String model;
    private String vehicleType;     // SEDAN, SUV...
    private String fuelType;        // DIESEL, ESSENCE...
    private String transmission;    // MANUAL, AUTOMATIC...
    private String listingMode;     // SALE_ONLY, RENTAL_ONLY, BOTH
    private String status;          // AVAILABLE...
    
    private Integer yearMin;
    private Integer yearMax;
    
    private BigDecimal minSalePrice;
    private BigDecimal maxSalePrice;
    
    private BigDecimal minRentalPrice;
    private BigDecimal maxRentalPrice;
    
    private Integer seats;
    private Boolean isFeatured;
    private String q;               // Recherche plein texte (Full text search)
    
    // Pagination
    private int page = 1;
    private int limit = 12;
}