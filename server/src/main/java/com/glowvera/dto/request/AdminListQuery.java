package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminListQuery(
        @Size(max = 100) String q,
        @Pattern(regexp = "^(all|active|inactive)$", message = "status must be all, active or inactive") String status,
        @Min(1) Integer page,
        @Min(1) @Max(100) Integer limit) {

    public AdminListQuery {
        q = Normalizers.trimToNull(q);
        status = status == null ? "all" : status;
        page = page == null ? 1 : page;
        limit = limit == null ? 20 : limit;
    }
}
