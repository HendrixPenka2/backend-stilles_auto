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
@Table("auth.refresh_tokens")
public class RefreshToken {
    @Id
    private UUID id;
    private UUID userId;
    private String token;
    private Instant expiryDate;

    @Builder.Default
    private boolean revoked = false;
}
