package com.glowvera.dto.response;

import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsViews {

    private AnalyticsViews() {
    }

    public record Day(LocalDate date, long revenueCents, long orders) {
    }

    public record StatusCount(OrderStatus status, long orders) {
    }

    public record TopProduct(String name, long units, long revenueCents) {
    }

    public record MethodRow(PaymentMethod method, long orders, long revenueCents) {
    }

    public record CouponRow(String code, long orders, long discountCents) {
    }

    /**
     * Revenue = orders that are paid or further along (PAID, PROCESSING, SHIPPED, DELIVERED), delivery included.
     * The "prev" numbers are the SAME number of days directly before, for the percentage change.
     */
    public record Summary(
            int days, LocalDate from, LocalDate to,
            long revenueCents, long orders, long averageOrderCents, long discountCents,
            long prevRevenueCents, long prevOrders,
            List<Day> daily, List<StatusCount> statuses, List<TopProduct> topProducts,
            List<MethodRow> methods, List<CouponRow> coupons) {
    }
}
