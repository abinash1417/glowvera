package com.glowvera.dto.response;

import com.glowvera.domain.DiscountType;
import java.time.Instant;

public final class CouponViews {

    private CouponViews() {
    }

    public record Item(
            Long id, String code, String description, DiscountType discountType, long discountValue,
            Long maxDiscountCents, long minOrderCents, Integer usageLimit, int perUserLimit,
            Instant startsAt, Instant expiresAt, boolean active, long usedCount, String state) {
    }
}
