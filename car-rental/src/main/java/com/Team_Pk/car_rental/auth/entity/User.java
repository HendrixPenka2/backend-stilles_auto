package com.Team_Pk.car_rental.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.CreatedDate;     // Import Spring
import org.springframework.data.annotation.LastModifiedDate; // Import Spring

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("auth.users")  // ← mappe exactement sur ton schéma auth.users
public class User {

    @Id
    private UUID id;

    private String email;
    private String passwordHash;   // on stocke le hash, jamais le mot de passe clair

    private String firstName;
    private String lastName;
    private String phone;

    private UserRole role;           // "CLIENT", "ADMIN"

    private boolean emailVerified;
    private boolean isActive;

    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}