package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.DiscountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CouponRequest(
        @NotBlank(message = "Coupon code is required")
        @Pattern(regexp = "^[A-Z0-9_-]{3,40}$", message = "Code must be 3-40 letters, numbers, - or _")
        String code,

        @Size(max = 150, message = "Description is too long")
        String description,

        @NotNull(message = "Choose a discount type")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @Min(value = 1, message = "Discount value must be at least 1")
        @Max(value = 100_000_000, message = "Discount value is too large")
        Long discountValue,

        @Min(value = 1, message = "Maximum discount must be positive")
        Long maxDiscountCents,

        @Min(value = 0, message = "Minimum order cannot be negative")
        Long minOrderCents,

        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit,

        @Min(value = 1, message = "Per-customer limit must be at least 1")
        @Max(value = 100, message = "Per-customer limit is too large")
        Integer perUserLimit,

        Instant startsAt,
        Instant expiresAt,
        Boolean active) {

    public CouponRequest {
        code = Normalizers.upper(code);
        description = Normalizers.trimToNull(description);
    }
}
