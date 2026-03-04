package com.Team_Pk.car_rental.community.entity;


import com.Team_Pk.car_rental.community.entity.enums.ReviewEntityType;
import com.Team_Pk.car_rental.community.entity.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reviews", schema = "community")
public class Review {
    @Id
    private UUID id;
    private UUID userId;
    private ReviewEntityType entityType;
    private UUID entityId;
    private Integer rating;
    private String title;
    private String comment;
    private ReviewStatus status;
    private String adminNote;
    private UUID moderatedBy;
    private Instant moderatedAt;
    
    @CreatedDate
    private Instant createdAt;
    @LastModifiedDate
    private Instant updatedAt;
}