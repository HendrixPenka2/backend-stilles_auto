package com.Team_Pk.car_rental.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;  // secondes
}