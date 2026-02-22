package com.Team_Pk.car_rental.catalog.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accessory_images", schema = "catalog")
public class AccessoryImage {
    @Id
    private UUID id;
    private UUID accessoryId;
    private String url;
    private String thumbnailUrl;
    private String altText;
    private Boolean isPrimary;
    private Integer displayOrder;
    private Instant uploadedAt;
}