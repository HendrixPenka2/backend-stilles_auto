package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.Team_Pk.car_rental.catalog.entity.Vehicle;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;

@Data
@Builder
public class VehicleDetailResponse {
    private Vehicle vehicle;           // Les infos de la voiture
    private List<VehicleImage> images; // La liste de ses photos
}