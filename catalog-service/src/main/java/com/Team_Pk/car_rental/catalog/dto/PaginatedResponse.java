package com.Team_Pk.car_rental.catalog.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PaginatedResponse<T> {
    private List<T> data;
    private Meta meta;

    @Data
    @Builder
    public static class Meta {
        private long total;
        private int page;
        private int limit;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrev;
    }
}
