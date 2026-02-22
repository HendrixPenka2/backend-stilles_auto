package com.Team_Pk.car_rental.catalog.dto;

import com.Team_Pk.car_rental.catalog.entity.enums.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VehicleRequest {
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
    private String features; // JSON en format String (ex: "[\"GPS\", \"Clim\"]")
    private Boolean isFeatured;
}