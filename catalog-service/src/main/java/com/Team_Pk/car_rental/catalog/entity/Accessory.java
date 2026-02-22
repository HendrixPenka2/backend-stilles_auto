package com.Team_Pk.car_rental.catalog.entity;

import com.Team_Pk.car_rental.catalog.entity.enums.AccessoryCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accessories", schema = "catalog")
public class Accessory {

    @Id
    private UUID id;

    private String name;
    private String brand;
    private String sku;
    private String category;
    private String subCategory;

    // Les tableaux PostgreSQL (TEXT[]) sont mappés en tableaux Java
    private String[] compatibleBrands;
    private String[] compatibleModels;

    private BigDecimal price;
    private BigDecimal comparePrice;
    
    private Integer stockQuantity;
    private Integer lowStockAlert;

    private AccessoryCondition condition;
    private BigDecimal weightKg;
    
    // Les JSONB stockés en String
    private String dimensions;
    private String description;
    private String specifications;

    private Boolean isFeatured;
    private Boolean isActive;
    
    private UUID createdBy;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}