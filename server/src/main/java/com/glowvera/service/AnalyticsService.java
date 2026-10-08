package com.glowvera.service;

import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.dto.response.AnalyticsViews;
import com.glowvera.repository.AnalyticsRepository;
import com.glowvera.repository.AnalyticsRepository.SaleRow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Colombo");

    private static final Set<OrderStatus> REVENUE = Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isCommitted)
            .collect(Collectors.toUnmodifiableSet());

    private final AnalyticsRepository repo;
    private final Clock clock;

    public AnalyticsService(AnalyticsRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public AnalyticsViews.Summary summary(int days) {
        LocalDate today = LocalDate.now(clock.withZone(SHOP_ZONE));
        LocalDate firstDay = today.minusDays(days - 1L);
        Instant from = firstDay.atStartOfDay(SHOP_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant();
        Instant prevFrom = firstDay.minusDays(days).atStartOfDay(SHOP_ZONE).toInstant();

        List<SaleRow> sales = repo.sales(REVENUE, from, to);

        // daily series: every day is present, even with no sales, so the chart has no gaps
        Map<LocalDate, long[]> perDay = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            perDay.put(firstDay.plusDays(i), new long[2]);
        }
        long revenue = 0;
        long discounts = 0;
        Map<PaymentMethod, long[]> perMethod = new EnumMap<>(PaymentMethod.class);
        Map<String, long[]> perCoupon = new LinkedHashMap<>();

        for (SaleRow s : sales) {
            LocalDate day = s.getCreatedAt().atZone(SHOP_ZONE).toLocalDate();
            long[] d = perDay.get(day);
            if (d != null) {
                d[0] += s.getTotalCents();
                d[1] += 1;
            }
            revenue += s.getTotalCents();
            discounts += s.getDiscountCents();

            long[] m = perMethod.computeIfAbsent(s.getPaymentMethod(), k -> new long[2]);
            m[0] += 1;
            m[1] += s.getTotalCents();

            if (s.getCouponCode() != null) {
                long[] c = perCoupon.computeIfAbsent(s.getCouponCode(), k -> new long[2]);
                c[0] += 1;
                c[1] += s.getDiscountCents();
            }
        }

        long orders = sales.size();
        AnalyticsRepository.Totals prev = repo.totals(REVENUE, prevFrom, from);

        return new AnalyticsViews.Summary(
                days, firstDay, today,
                revenue, orders, orders == 0 ? 0 : revenue / orders, discounts,
                prev.getRevenue() == null ? 0 : prev.getRevenue(),
                prev.getOrders() == null ? 0 : prev.getOrders(),
                perDay.entrySet().stream()
                        .map(e -> new AnalyticsViews.Day(e.getKey(), e.getValue()[0], e.getValue()[1]))
                        .toList(),
                repo.statusCounts(from, to).stream()
                        .map(r -> new AnalyticsViews.StatusCount(r.getStatus(), r.getOrders()))
                        .sorted(Comparator.comparing(AnalyticsViews.StatusCount::status))
                        .toList(),
                repo.topProducts(REVENUE, from, to, PageRequest.of(0, 5)).stream()
                        .map(r -> new AnalyticsViews.TopProduct(r.getName(), r.getUnits(), r.getRevenue()))
                        .toList(),
                perMethod.entrySet().stream()
                        .map(e -> new AnalyticsViews.MethodRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                        .toList(),
                perCoupon.entrySet().stream()
                        .map(e -> new AnalyticsViews.CouponRow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                        .sorted(Comparator.comparingLong(AnalyticsViews.CouponRow::discountCents).reversed())
                        .toList());
    }
}
