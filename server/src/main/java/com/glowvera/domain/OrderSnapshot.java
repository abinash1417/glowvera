package com.glowvera.domain;

import java.time.Instant;
import java.util.List;

public record OrderSnapshot(
        String orderCode,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String customerName,
        String customerEmail,
        String customerPhone,
        String addressLine,
        String city,
        String district,
        long subtotalCents,
        long discountCents,
        long shippingCents,
        long totalCents,
        String currency,
        String couponCode,
        Instant reservationExpiresAt,
        String notes,
        Instant createdAt,
        List<Item> items) {

    public record Item(
            String productName,
            String variantName,
            long unitPriceCents,
            int quantity,
            long lineTotalCents) {
    }
}
