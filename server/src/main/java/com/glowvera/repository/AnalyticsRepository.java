package com.glowvera.repository;

import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.entity.CustomerOrder;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AnalyticsRepository extends Repository<CustomerOrder, Long> {

    interface SaleRow {
        Instant getCreatedAt();

        long getTotalCents();

        long getDiscountCents();

        PaymentMethod getPaymentMethod();

        String getCouponCode();
    }

    interface Totals {
        Long getOrders();

        Long getRevenue();
    }

    interface StatusRow {
        OrderStatus getStatus();

        Long getOrders();
    }

    interface ProductRow {
        String getName();

        Long getUnits();

        Long getRevenue();
    }

    @Query("select o.createdAt as createdAt, o.totalCents as totalCents, o.discountCents as discountCents, "
            + "o.paymentMethod as paymentMethod, o.couponCode as couponCode "
            + "from CustomerOrder o where o.status in :statuses and o.createdAt >= :from and o.createdAt < :to")
    List<SaleRow> sales(@Param("statuses") Collection<OrderStatus> statuses,
                        @Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(o) as orders, coalesce(sum(o.totalCents), 0) as revenue "
            + "from CustomerOrder o where o.status in :statuses and o.createdAt >= :from and o.createdAt < :to")
    Totals totals(@Param("statuses") Collection<OrderStatus> statuses,
                  @Param("from") Instant from, @Param("to") Instant to);

    @Query("select o.status as status, count(o) as orders from CustomerOrder o "
            + "where o.createdAt >= :from and o.createdAt < :to group by o.status")
    List<StatusRow> statusCounts(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select i.productName as name, sum(i.quantity) as units, sum(i.lineTotalCents) as revenue "
            + "from OrderItem i where i.order.status in :statuses "
            + "and i.order.createdAt >= :from and i.order.createdAt < :to "
            + "group by i.productName order by sum(i.lineTotalCents) desc")
    List<ProductRow> topProducts(@Param("statuses") Collection<OrderStatus> statuses,
                                 @Param("from") Instant from, @Param("to") Instant to, Pageable limit);
}
