package com.Team_Pk.car_rental.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
//(Ce que le frontend reçoit pour afficher le calendrier)
public class AvailabilityResponse {
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
