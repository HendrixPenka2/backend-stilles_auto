package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.Team_Pk.car_rental.catalog.entity.enums.FuelType;
import com.Team_Pk.car_rental.catalog.entity.enums.TransmissionType;
import com.Team_Pk.car_rental.catalog.entity.enums.VehicleType;

@Data
@Builder
public class FilterOptionsResponse {
    private List<VehicleType> vehicleTypes;
    private List<FuelType> fuelTypes;
    private List<TransmissionType> transmissions;
    private MinMax year;
    private MinMax salePrice;
    private MinMax rentalPrice;

    @Data
    @Builder
    public static class MinMax {
        private Number min;
        private Number max;
    }
}