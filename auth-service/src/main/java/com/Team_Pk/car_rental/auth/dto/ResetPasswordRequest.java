package com.Team_Pk.car_rental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;
    @NotBlank @Size(min = 8)
    private String newPassword;
    
    // Nettoyer automatiquement le token
    public void setToken(String token) {
        this.token = token != null ? token.trim() : null;
    }
}
