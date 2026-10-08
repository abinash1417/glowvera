package com.glowvera.domain;

// Pure discount arithmetic (no Spring, no database), so it is easy to unit test. //
public final class CouponMath {

    private CouponMath() {
    }


    public static long discount(DiscountType type, long value, Long maxCents, long subtotalCents) {
        long raw = type == DiscountType.PERCENT ? subtotalCents * value / 100 : value;
        if (type == DiscountType.PERCENT && maxCents != null) {
            raw = Math.min(raw, maxCents);
        }
        return Math.max(0, Math.min(raw, subtotalCents));
    }
}
