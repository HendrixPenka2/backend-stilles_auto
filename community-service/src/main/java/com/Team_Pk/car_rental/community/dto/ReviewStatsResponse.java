package com.Team_Pk.car_rental.community.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewStatsResponse {
    private long totalReviews;
    private long pendingCount;
    private double averageGlobalRating;
}