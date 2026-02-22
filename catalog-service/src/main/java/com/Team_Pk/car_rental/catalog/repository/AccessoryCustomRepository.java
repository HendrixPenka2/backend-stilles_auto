package com.Team_Pk.car_rental.catalog.repository;

import com.Team_Pk.car_rental.catalog.dto.AccessoryFilterOptions;
import com.Team_Pk.car_rental.catalog.dto.AccessorySearchCriteria;
import com.Team_Pk.car_rental.catalog.entity.Accessory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccessoryCustomRepository {
    Flux<Accessory> findAccessoriesByCriteria(AccessorySearchCriteria criteria);
    Mono<Long> countAccessoriesByCriteria(AccessorySearchCriteria criteria);
    Mono<AccessoryFilterOptions> getFilterOptions();
}