package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.OrderStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminOrderListQuery(
        OrderStatus status,
        @Size(max = 100) String q,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer limit) {

    public AdminOrderListQuery {
        q = Normalizers.trimToNull(q);
        page = page == null ? 1 : page;
        limit = limit == null ? 20 : limit;
    }
}
