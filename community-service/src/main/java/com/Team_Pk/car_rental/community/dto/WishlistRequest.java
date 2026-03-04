package com.Team_Pk.car_rental.community.dto;

import com.Team_Pk.car_rental.community.entity.enums.WishlistEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class WishlistRequest {
    @NotNull private WishlistEntityType entityType;
    @NotNull private UUID entityId;
}