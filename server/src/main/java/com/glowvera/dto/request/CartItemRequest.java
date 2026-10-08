package com.glowvera.dto.request;

import com.glowvera.domain.CartRules;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(
        @NotNull(message = "variantId is required") @Positive Long variantId,
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = CartRules.MAX_QTY_PER_LINE, message = "You can order at most 50 units of one item")
        Integer quantity) {

    public CartRules.Line toLine() {
        return new CartRules.Line(variantId, quantity);
    }
}
