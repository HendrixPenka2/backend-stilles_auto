package com.Team_Pk.car_rental.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

import com.Team_Pk.car_rental.community.entity.enums.ReviewEntityType;

@Data
public class ReviewRequest {
    @NotNull private ReviewEntityType entityType;
    @NotNull private UUID entityId;
    @Min(1) @Max(5) private Integer rating;
    @NotBlank private String title;
    @NotBlank private String comment;
}
