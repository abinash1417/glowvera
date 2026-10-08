package com.glowvera.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CouponMathTest {

    @Test
    void percentIsTakenFromSubtotal() {
        assertEquals(50000, CouponMath.discount(DiscountType.PERCENT, 10, null, 500000));
    }

    @Test
    void percentRespectsTheCap() {
        assertEquals(30000, CouponMath.discount(DiscountType.PERCENT, 20, 30000L, 500000));
    }

    @Test
    void fixedAmountIsUsedAsIs() {
        assertEquals(20000, CouponMath.discount(DiscountType.FIXED, 20000, null, 500000));
    }

    @Test
    void discountNeverExceedsTheSubtotal() {
        assertEquals(15000, CouponMath.discount(DiscountType.FIXED, 99999, null, 15000));
        assertEquals(15000, CouponMath.discount(DiscountType.PERCENT, 100, null, 15000));
    }

    @Test
    void percentRoundsDownToWholeCents() {
        assertEquals(33, CouponMath.discount(DiscountType.PERCENT, 10, null, 333));
    }
}
