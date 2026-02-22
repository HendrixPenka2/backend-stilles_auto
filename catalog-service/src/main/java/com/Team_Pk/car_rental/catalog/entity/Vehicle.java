package com.Team_Pk.car_rental.catalog.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import com.Team_Pk.car_rental.catalog.entity.enums.FuelType;
import com.Team_Pk.car_rental.catalog.entity.enums.ListingMode;
import com.Team_Pk.car_rental.catalog.entity.enums.TransmissionType;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleStatus;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicles", schema = "catalog")
public class Vehicle {

    @Id
    private UUID id;
    
    private String title;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private String vin;
    
    private VehicleType vehicleType;
    private FuelType fuelType;
    private TransmissionType transmission;
    
    private Integer mileage;
    private BigDecimal engineCapacity;
    private Integer horsepower;
    private Integer doors;
    private Integer seats;
    
    private ListingMode listingMode;
    
    private BigDecimal salePrice;
    private BigDecimal rentalPricePerDay;
    
    private Integer stockQuantity;
    private VehicleStatus status;
    
    private String description;
    
    // Pour le JSONB, on peut le stocker sous forme de String simple pour le manipuler facilement
    private String features; 
    
    private Boolean isFeatured;
    private Boolean isActive;
    
    private UUID createdBy; // ID de l'admin
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
}