package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VehicleCalendarResponse {
    private UUID vehicleId;
    private BigDecimal rentalPricePerDay;
    
    private List<AvailabilityResponse> blockedPeriods; // Ce que tu as déjà
    private List<Period> availablePeriods;             // Ce qui manque

    @Data
    @Builder
    public static class Period {
        private LocalDate start;
        private LocalDate end;
    }
}