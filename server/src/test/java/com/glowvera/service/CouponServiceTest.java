package com.glowvera.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import com.glowvera.domain.DiscountType;
import com.glowvera.dto.request.CouponRequest;
import com.glowvera.dto.response.CouponViews;
import com.glowvera.entity.Coupon;
import com.glowvera.repository.CouponRepository;
import com.glowvera.repository.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the coupon rules. Repositories are mocked, so no database is needed. */
class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final long USER = 7L;
    private static final long SUBTOTAL = 500_000;   // Rs. 5,000.00

    private CouponRepository coupons;
    private OrderRepository orders;
    private CouponService service;

    @BeforeEach
    void setUp() {
        coupons = mock(CouponRepository.class);
        orders = mock(OrderRepository.class);
        service = new CouponService(coupons, orders, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // ───────── helpers ─────────

    private Coupon coupon() {
        Coupon c = new Coupon();
        c.setCode("WELCOME10");
        c.setDiscountType(DiscountType.PERCENT);
        c.setDiscountValue(10);
        c.setPerUserLimit(1);
        c.setActive(true);
        return c;
    }

    private Coupon stored(Coupon c) {
        when(coupons.findByCode(c.getCode())).thenReturn(Optional.of(c));
        return c;
    }

    private String refusal(long subtotal, Long userId) {
        AppException e = assertThrows(AppException.class, () -> service.preview("WELCOME10", subtotal, userId));
        assertEquals(ErrorCode.BAD_REQUEST, e.getCode());
        return e.getMessage();
    }

    private static CouponRequest request(DiscountType type, long value, Instant starts, Instant expires) {
        return new CouponRequest("SAVE20", "Test", type, value, null, null, null, null, starts, expires, true);
    }

    // ───────── customer rules ─────────

    @Test
    void unknownCodeIsRejected() {
        when(coupons.findByCode("NOPE")).thenReturn(Optional.empty());
        AppException e = assertThrows(AppException.class, () -> service.preview("NOPE", SUBTOTAL, USER));
        assertEquals("This coupon code is not valid", e.getMessage());
    }

    @Test
    void inactiveCouponLooksLikeAnUnknownCode() {
        Coupon c = stored(coupon());
        c.setActive(false);
        assertEquals("This coupon code is not valid", refusal(SUBTOTAL, USER));
    }

    @Test
    void guestsMustLogInFirst() {
        stored(coupon());
        assertEquals("Please log in to use a coupon code", refusal(SUBTOTAL, null));
    }

    @Test
    void couponThatHasNotStartedIsRefused() {
        stored(coupon()).setStartsAt(NOW.plusSeconds(60));
        assertEquals("This coupon is not active yet", refusal(SUBTOTAL, USER));
    }

    @Test
    void expiredCouponIsRefused() {
        stored(coupon()).setExpiresAt(NOW.minusSeconds(60));
        assertEquals("This coupon has expired", refusal(SUBTOTAL, USER));
    }

    @Test
    void couponIsExpiredAtTheExactExpiryInstant() {
        stored(coupon()).setExpiresAt(NOW);
        assertEquals("This coupon has expired", refusal(SUBTOTAL, USER));
    }

    @Test
    void minimumOrderIsCheckedOnTheSubtotal() {
        stored(coupon()).setMinOrderCents(200_000);
        assertTrue(refusal(150_000, USER).contains("Rs. 2,000.00"));
    }

    @Test
    void orderExactlyAtTheMinimumIsAccepted() {
        stored(coupon()).setMinOrderCents(SUBTOTAL);
        assertEquals(50_000, service.preview("WELCOME10", SUBTOTAL, USER).discountCents());
    }

    @Test
    void totalUsageLimitStopsFurtherUse() {
        Coupon c = stored(coupon());
        c.setUsageLimit(100);
        when(orders.countByCouponCodeAndStatusNotIn(eq("WELCOME10"), anyCollection())).thenReturn(100L);
        assertEquals("This coupon has been fully used", refusal(SUBTOTAL, USER));
    }

    @Test
    void lastRemainingUseIsStillAccepted() {
        Coupon c = stored(coupon());
        c.setUsageLimit(100);
        when(orders.countByCouponCodeAndStatusNotIn(eq("WELCOME10"), anyCollection())).thenReturn(99L);
        assertEquals(50_000, service.preview("WELCOME10", SUBTOTAL, USER).discountCents());
    }

    @Test
    void perCustomerLimitStopsRepeatUse() {
        stored(coupon());
        when(orders.countByCouponCodeAndUserIdAndStatusNotIn(eq("WELCOME10"), eq(USER), anyCollection()))
                .thenReturn(1L);
        assertEquals("You have already used this coupon", refusal(SUBTOTAL, USER));
    }

    @Test
    void anotherCustomerCanStillUseTheCoupon() {
        stored(coupon());
        when(orders.countByCouponCodeAndUserIdAndStatusNotIn(eq("WELCOME10"), eq(USER), anyCollection()))
                .thenReturn(1L);
        when(orders.countByCouponCodeAndUserIdAndStatusNotIn(eq("WELCOME10"), eq(99L), anyCollection()))
                .thenReturn(0L);
        assertEquals(50_000, service.preview("WELCOME10", SUBTOTAL, 99L).discountCents());
    }

    @Test
    void percentCouponReturnsTheDiscountAndTheCoupon() {
        stored(coupon());
        CouponService.Applied applied = service.preview("WELCOME10", SUBTOTAL, USER);
        assertEquals(50_000, applied.discountCents());
        assertEquals("WELCOME10", applied.coupon().getCode());
    }

    @Test
    void percentCouponRespectsItsCap() {
        Coupon c = stored(coupon());
        c.setDiscountValue(50);
        c.setMaxDiscountCents(30_000L);
        assertEquals(30_000, service.preview("WELCOME10", SUBTOTAL, USER).discountCents());
    }

    @Test
    void fixedCouponNeverExceedsTheSubtotal() {
        Coupon c = stored(coupon());
        c.setDiscountType(DiscountType.FIXED);
        c.setDiscountValue(900_000);
        assertEquals(SUBTOTAL, service.preview("WELCOME10", SUBTOTAL, USER).discountCents());
    }

    @Test
    void checkoutLocksTheCouponRowAndAppliesTheSameRules() {
        when(coupons.findByCodeForUpdate("WELCOME10")).thenReturn(Optional.of(coupon()));
        assertEquals(50_000, service.applyForCheckout("WELCOME10", SUBTOTAL, USER).discountCents());
        verify(coupons, never()).findByCode(any());
    }

    @Test
    void checkoutRejectsAnUnknownCode() {
        when(coupons.findByCodeForUpdate("NOPE")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> service.applyForCheckout("NOPE", SUBTOTAL, USER));
    }

    // ───────── admin rules ─────────

    @Test
    void createRejectsADuplicateCode() {
        when(coupons.existsByCode("SAVE20")).thenReturn(true);
        AppException e = assertThrows(AppException.class,
                () -> service.create(request(DiscountType.PERCENT, 20, null, null)));
        assertEquals(ErrorCode.CONFLICT, e.getCode());
        verify(coupons, never()).save(any());
    }

    @Test
    void createRejectsAPercentageAbove100() {
        assertThrows(AppException.class, () -> service.create(request(DiscountType.PERCENT, 101, null, null)));
        verify(coupons, never()).save(any());
    }

    @Test
    void createRejectsAnExpiryBeforeTheStart() {
        assertThrows(AppException.class,
                () -> service.create(request(DiscountType.FIXED, 10_000, NOW.plusSeconds(100), NOW)));
    }

    @Test
    void createSavesWithSensibleDefaults() {
        when(coupons.existsByCode("SAVE20")).thenReturn(false);
        when(coupons.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        CouponViews.Item item = service.create(request(DiscountType.PERCENT, 20, null, null));

        assertEquals("SAVE20", item.code());
        assertEquals(0, item.minOrderCents());
        assertEquals(1, item.perUserLimit());
        assertEquals("ACTIVE", item.state());
        assertEquals(0, item.usedCount());
    }

    @Test
    void updateCannotChangeTheCode() {
        Coupon existing = coupon();
        when(coupons.findById(1L)).thenReturn(Optional.of(existing));

        service.update(1L, request(DiscountType.FIXED, 10_000, null, null));   // request says SAVE20

        assertEquals("WELCOME10", existing.getCode());
        assertEquals(DiscountType.FIXED, existing.getDiscountType());
    }

    @Test
    void percentCapIsDroppedWhenSwitchingToAFixedCoupon() {
        Coupon existing = coupon();
        existing.setMaxDiscountCents(30_000L);
        when(coupons.findById(1L)).thenReturn(Optional.of(existing));

        CouponRequest r = new CouponRequest("WELCOME10", null, DiscountType.FIXED, 10_000L, 30_000L,
                null, null, null, null, null, true);
        service.update(1L, r);

        assertNull(existing.getMaxDiscountCents());
    }

    @Test
    void updateOfAMissingCouponIsNotFound() {
        when(coupons.findById(anyLong())).thenReturn(Optional.empty());
        AppException e = assertThrows(AppException.class,
                () -> service.update(5L, request(DiscountType.PERCENT, 10, null, null)));
        assertEquals(ErrorCode.NOT_FOUND, e.getCode());
    }

    // ───────── list / state ─────────

    private CouponViews.Item onlyItem(Coupon c, long used) {
        OrderRepository.CouponUse use = mock(OrderRepository.CouponUse.class);
        when(use.getCode()).thenReturn(c.getCode());
        when(use.getUses()).thenReturn(used);
        when(orders.couponUsage(anyCollection())).thenReturn(List.of(use));
        when(coupons.findAllByOrderByIdDesc()).thenReturn(List.of(c));
        return service.list().get(0);
    }

    @Test
    void listShowsTheUsageCountAndAnActiveState() {
        CouponViews.Item item = onlyItem(coupon(), 3);
        assertEquals(3, item.usedCount());
        assertEquals("ACTIVE", item.state());
    }

    @Test
    void stateIsExhaustedWhenTheLimitIsReached() {
        Coupon c = coupon();
        c.setUsageLimit(3);
        assertEquals("EXHAUSTED", onlyItem(c, 3).state());
    }

    @Test
    void stateIsExpiredScheduledOrInactive() {
        Coupon expired = coupon();
        expired.setExpiresAt(NOW.minusSeconds(1));
        assertEquals("EXPIRED", onlyItem(expired, 0).state());

        Coupon scheduled = coupon();
        scheduled.setStartsAt(NOW.plusSeconds(1));
        assertEquals("SCHEDULED", onlyItem(scheduled, 0).state());

        Coupon off = coupon();
        off.setActive(false);
        assertEquals("INACTIVE", onlyItem(off, 0).state());
    }
}