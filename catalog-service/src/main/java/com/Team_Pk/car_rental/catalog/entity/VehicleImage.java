package com.Team_Pk.car_rental.catalog.entity;

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
@Table(name = "vehicle_images", schema = "catalog")
public class VehicleImage {

    @Id
    private UUID id;
    
    private UUID vehicleId;
    private String url;
    private String thumbnailUrl;
    private String altText;
    private Boolean isPrimary;
    private Integer displayOrder;
    private Long fileSizeBytes;
    private Instant uploadedAt;
}
