package com.Team_Pk.car_rental.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("auth.password_reset_tokens")
public class PasswordResetToken {
    @Id
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiryDate;
}
