package com.Team_Pk.car_rental.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phone;
    @Size(min = 8)
    private String newPassword;
}