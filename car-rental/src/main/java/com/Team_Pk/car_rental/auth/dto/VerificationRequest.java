package com.Team_Pk.car_rental.auth.dto;

import lombok.Data;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class VerificationRequest {
    @NotNull(message = "L'ID utilisateur est requis")
    private UUID userId;

    @NotBlank(message = "Le code de vérification est requis")
    private String token;
}
