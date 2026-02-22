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
@Table("auth.email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private UUID id;

    private UUID userId;
    private String token;         // code OTP, ex: "123456"
    private Instant expiryDate;   // expire après 15 min
}