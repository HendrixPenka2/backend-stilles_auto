package com.Team_Pk.car_rental.catalog.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.Team_Pk.car_rental.catalog.entity.Accessory;

public interface AccessoryRepository extends ReactiveCrudRepository<Accessory, UUID>, AccessoryCustomRepository {
}