package com.Team_Pk.car_rental.catalog.dto;

import lombok.Data;
import java.math.BigDecimal;
//(Pour les filtres URL)
@Data
public class AccessorySearchCriteria {
    private String q;               // Recherche texte global
    private String category;
    private String subCategory;
    private String brand;
    private String condition;
    
    private String compatibleBrand;
    private String compatibleModel;
    
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    private Boolean inStock;
    private Boolean isFeatured;
    
    // Pagination
    private int page = 1;
    private int limit = 12;
}