package com.Team_Pk.car_rental.catalog.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BlockPeriodRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason; // "MAINTENANCE", "OTHER"
    private String notes;
}