package com.glowvera.repository;

import com.glowvera.entity.OrderItemBatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemBatchRepository extends JpaRepository<OrderItemBatch, Long> {

    @Query("select a from OrderItemBatch a join fetch a.orderItem i join fetch a.batch "
            + "where i.order.id = :orderId")
    List<OrderItemBatch> findByOrderId(@Param("orderId") Long orderId);
}
