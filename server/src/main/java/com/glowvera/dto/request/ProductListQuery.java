package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductListQuery(
        @Size(max = 100) String q,
        @Size(max = 100) String category,
        @Size(max = 50) String skinType,
        @Size(max = 50) String concern,
        @Min(0) Long minPrice,
        @Min(0) Long maxPrice,
        @Pattern(regexp = "^(newest|price_asc|price_desc|name)$", message = "Unknown sort option") String sort,
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer limit) {

    public ProductListQuery {
        q = Normalizers.trimToNull(q);
        category = Normalizers.trimToNull(category);
        skinType = Normalizers.trimToNull(skinType);
        concern = Normalizers.trimToNull(concern);
        sort = sort == null ? "newest" : sort;
        page = page == null ? 1 : page;
        limit = limit == null ? 12 : limit;
    }
}
