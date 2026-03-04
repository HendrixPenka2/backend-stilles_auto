package com.Team_Pk.car_rental.community.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.Team_Pk.car_rental.community.entity.enums.WishlistEntityType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "wishlist_items", schema = "community")
public class WishlistItem {
    @Id
    private UUID id;
    private UUID userId;
    private WishlistEntityType entityType;
    private UUID entityId;
    
    @CreatedDate
    private Instant addedAt;
}