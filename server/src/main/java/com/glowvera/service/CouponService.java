package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.CouponMath;
import com.glowvera.domain.DiscountType;
import com.glowvera.domain.Money;
import com.glowvera.domain.OrderStatus;
import com.glowvera.dto.request.CouponRequest;
import com.glowvera.dto.response.CouponViews;
import com.glowvera.entity.Coupon;
import com.glowvera.repository.CouponRepository;
import com.glowvera.repository.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class CouponService {

    private static final Set<OrderStatus> NOT_COUNTED = Set.of(OrderStatus.CANCELLED, OrderStatus.FAILED);

    public record Applied(Coupon coupon, long discountCents) {
    }

    private final CouponRepository coupons;
    private final OrderRepository orders;
    private final Clock clock;

    public CouponService(CouponRepository coupons, OrderRepository orders, Clock clock) {
        this.coupons = coupons;
        this.orders = orders;
        this.clock = clock;
    }


    @Transactional(readOnly = true)
    public Applied preview(String code, long subtotalCents, Long userId) {
        Coupon coupon = coupons.findByCode(code).orElseThrow(CouponService::invalid);
        return evaluate(coupon, subtotalCents, userId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Applied applyForCheckout(String code, long subtotalCents, Long userId) {
        Coupon coupon = coupons.findByCodeForUpdate(code).orElseThrow(CouponService::invalid);
        return evaluate(coupon, subtotalCents, userId);
    }

    private Applied evaluate(Coupon c, long subtotalCents, Long userId) {
        if (userId == null) {
            throw AppException.badRequest("Please log in to use a coupon code");
        }
        Instant now = Instant.now(clock);
        if (!c.isActive()) {
            throw invalid();
        }
        if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) {
            throw AppException.badRequest("This coupon is not active yet");
        }
        if (c.getExpiresAt() != null && !now.isBefore(c.getExpiresAt())) {
            throw AppException.badRequest("This coupon has expired");
        }
        if (subtotalCents < c.getMinOrderCents()) {
            throw AppException.badRequest(
                    "Spend at least " + Money.formatRupees(c.getMinOrderCents()) + " to use this coupon");
        }
        if (c.getUsageLimit() != null
                && orders.countByCouponCodeAndStatusNotIn(c.getCode(), NOT_COUNTED) >= c.getUsageLimit()) {
            throw AppException.badRequest("This coupon has been fully used");
        }
        if (orders.countByCouponCodeAndUserIdAndStatusNotIn(c.getCode(), userId, NOT_COUNTED) >= c.getPerUserLimit()) {
            throw AppException.badRequest("You have already used this coupon");
        }
        long discount = CouponMath.discount(
                c.getDiscountType(), c.getDiscountValue(), c.getMaxDiscountCents(), subtotalCents);
        return new Applied(c, discount);
    }

    private static AppException invalid() {
        return AppException.badRequest("This coupon code is not valid");
    }


    @Transactional(readOnly = true)
    public List<CouponViews.Item> list() {
        Map<String, Long> uses = orders.couponUsage(NOT_COUNTED).stream()
                .collect(Collectors.toMap(u -> u.getCode(), u -> u.getUses()));
        return coupons.findAllByOrderByIdDesc().stream()
                .map(c -> toView(c, uses.getOrDefault(c.getCode(), 0L)))
                .toList();
    }

    @Transactional
    public CouponViews.Item create(CouponRequest r) {
        validate(r);
        if (coupons.existsByCode(r.code())) {
            throw AppException.conflict("A coupon with this code already exists");
        }
        Coupon c = new Coupon();
        c.setCode(r.code());
        copy(r, c);
        return toView(coupons.save(c), 0);
    }

    @Transactional
    public CouponViews.Item update(Long id, CouponRequest r) {
        validate(r);
        Coupon c = coupons.findById(id).orElseThrow(() -> AppException.notFound("Coupon not found"));
        copy(r, c);   // the code itself never changes: past orders refer to it
        long uses = orders.countByCouponCodeAndStatusNotIn(c.getCode(), NOT_COUNTED);
        return toView(c, uses);
    }

    private void validate(CouponRequest r) {
        if (r.discountType() == DiscountType.PERCENT && (r.discountValue() < 1 || r.discountValue() > 100)) {
            throw AppException.badRequest("A percentage discount must be between 1 and 100");
        }
        if (r.startsAt() != null && r.expiresAt() != null && !r.expiresAt().isAfter(r.startsAt())) {
            throw AppException.badRequest("The expiry must be after the start time");
        }
    }

    private void copy(CouponRequest r, Coupon c) {
        c.setDescription(r.description());
        c.setDiscountType(r.discountType());
        c.setDiscountValue(r.discountValue());
        c.setMaxDiscountCents(r.discountType() == DiscountType.PERCENT ? r.maxDiscountCents() : null);
        c.setMinOrderCents(r.minOrderCents() == null ? 0 : r.minOrderCents());
        c.setUsageLimit(r.usageLimit());
        c.setPerUserLimit(r.perUserLimit() == null ? 1 : r.perUserLimit());
        c.setStartsAt(r.startsAt());
        c.setExpiresAt(r.expiresAt());
        c.setActive(r.active() == null || r.active());
    }

    private CouponViews.Item toView(Coupon c, long used) {
        Instant now = Instant.now(clock);
        String state;
        if (!c.isActive()) {
            state = "INACTIVE";
        } else if (c.getExpiresAt() != null && !now.isBefore(c.getExpiresAt())) {
            state = "EXPIRED";
        } else if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) {
            state = "SCHEDULED";
        } else if (c.getUsageLimit() != null && used >= c.getUsageLimit()) {
            state = "EXHAUSTED";
        } else {
            state = "ACTIVE";
        }
        return new CouponViews.Item(
                c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
                c.getMaxDiscountCents(), c.getMinOrderCents(), c.getUsageLimit(), c.getPerUserLimit(),
                c.getStartsAt(), c.getExpiresAt(), c.isActive(), used, state);
    }
}
