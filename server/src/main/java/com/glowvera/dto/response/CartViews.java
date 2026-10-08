package com.glowvera.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.glowvera.domain.CartRules.LineStatus;
import java.util.List;

public final class CartViews {

    private CartViews() {
    }

    public record District(String district, long feeCents) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuoteLine(
            Long variantId, String productName, String variantName, Long unitPriceCents,
            int quantity, Long lineTotalCents, Integer available, LineStatus status) {
    }

    // couponCode = the coupon that was applied (null if none). couponError = why a typed coupon was refused. //
    public record Quote(
            List<QuoteLine> lines, long subtotalCents, Long shippingCents, long totalCents, boolean canCheckout,
            String couponCode, long discountCents, String couponError) {
    }
}
