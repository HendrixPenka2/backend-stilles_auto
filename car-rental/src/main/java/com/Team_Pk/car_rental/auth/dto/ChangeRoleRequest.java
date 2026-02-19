package com.Team_Pk.car_rental.auth.dto;

import com.Team_Pk.car_rental.auth.entity.UserRole;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    private UserRole newRole;
}
