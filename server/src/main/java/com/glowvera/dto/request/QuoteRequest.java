package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuoteRequest(
        @NotEmpty(message = "Your cart is empty")
        @Size(max = 30, message = "Too many different items in one order")
        List<@Valid CartItemRequest> items,

        @Size(max = 40) String district,

        @Size(max = 40, message = "Coupon code is too long") String couponCode) {

    public QuoteRequest {
        district = Normalizers.trimToNull(district);
        couponCode = Normalizers.trimToNull(Normalizers.upper(couponCode));
    }
}
