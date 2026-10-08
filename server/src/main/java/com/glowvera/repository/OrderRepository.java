package com.glowvera.repository;

import com.glowvera.domain.OrderStatus;
import com.glowvera.entity.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

    Optional<CustomerOrder> findByOrderCode(String orderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

    List<CustomerOrder> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByCouponCodeAndStatusNotIn(String couponCode, Collection<OrderStatus> excluded);

    long countByCouponCodeAndUserIdAndStatusNotIn(String couponCode, Long userId, Collection<OrderStatus> excluded);

    interface CouponUse {
        String getCode();

        Long getUses();
    }

    @Query("select o.couponCode as code, count(o) as uses from CustomerOrder o "
            + "where o.couponCode is not null and o.status not in :excluded group by o.couponCode")
    List<CouponUse> couponUsage(@Param("excluded") Collection<OrderStatus> excluded);

    @Query("select o.id from CustomerOrder o "
            + "where o.status in :statuses and o.reservationExpiresAt < :now order by o.id")
    List<Long> findExpiredPendingIds(
            @Param("statuses") Collection<OrderStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
