package com.Team_Pk.car_rental.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String action; // ex: "VERIFIED", "PASSWORD_CHANGED"
}