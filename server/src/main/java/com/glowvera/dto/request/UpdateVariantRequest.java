package com.glowvera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateVariantRequest(
        @Size(min = 1, max = 80, message = "Variant name is required") String name,
        @Min(value = 100, message = "Price is too low") @Max(value = 100_000_000, message = "Price is too high")
        Long priceCents,
        @Min(0) @Max(1000) Integer lowStockThreshold,
        @JsonProperty("isActive") Boolean isActive) {

    public UpdateVariantRequest {
        name = Normalizers.trim(name);
    }

    public boolean isEmpty() {
        return name == null && priceCents == null && lowStockThreshold == null && isActive == null;
    }
}
