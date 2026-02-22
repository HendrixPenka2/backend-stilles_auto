package com.Team_Pk.car_rental.catalog.dto;

//(Pour Création/Modif Admin)
import lombok.Data;
import java.math.BigDecimal;

import com.Team_Pk.car_rental.catalog.entity.enums.AccessoryCondition;

@Data
public class AccessoryRequest {
    private String name;
    private String brand;
    private String sku;
    private String category;
    private String subCategory;
    
    // Tableaux de chaînes pour la compatibilité
    private String[] compatibleBrands;
    private String[] compatibleModels;
    
    private BigDecimal price;
    private BigDecimal comparePrice;
    private Integer stockQuantity;
    private Integer lowStockAlert;
    
    private AccessoryCondition condition; // NEW, LIKE_NEW...
    private BigDecimal weightKg;
    
    // JSON stockés en String
    private String dimensions;
    private String description;
    private String specifications;
    
    private Boolean isFeatured;
    private Boolean isActive;
}