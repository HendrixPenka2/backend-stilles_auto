package com.Team_Pk.car_rental.community.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingStats {
    private Double averageRating;
    private Integer reviewCount;
}