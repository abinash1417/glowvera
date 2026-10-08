package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVariantRequest(
        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,50}$", message = "SKU must be 3-50 letters, numbers or dashes")
        String sku,

        @NotBlank(message = "Variant name is required")
        @Size(max = 80)
        String name,

        @NotNull(message = "Price is required")
        @Min(value = 100, message = "Price is too low")
        @Max(value = 100_000_000, message = "Price is too high")
        Long priceCents,

        @Min(0) @Max(1000) Integer lowStockThreshold) {

    public CreateVariantRequest {
        sku = Normalizers.upper(sku);
        name = Normalizers.trim(name);
    }
}
