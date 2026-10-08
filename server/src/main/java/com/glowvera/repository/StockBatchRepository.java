package com.glowvera.repository;

import com.glowvera.entity.StockBatch;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    Optional<StockBatch> findByVariantIdAndBatchCode(Long variantId, String batchCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBatch b where b.variant.id = :variantId and b.quantity > 0 "
            + "order by b.expiryDate asc, b.id asc")
    List<StockBatch> lockPositiveBatches(@Param("variantId") Long variantId);

    // Units per variant that have NOT expired. Expired stock is never sold. //
    @Query("select new com.glowvera.repository.SellableQty(b.variant.id, sum(b.quantity)) "
            + "from StockBatch b "
            + "where b.variant.id in :ids and b.quantity > 0 and b.expiryDate >= :today "
            + "group by b.variant.id")
    List<SellableQty> sellableByVariant(@Param("ids") Collection<Long> ids, @Param("today") LocalDate today);

    @Query("select b from StockBatch b join fetch b.variant v join fetch v.product "
            + "where b.quantity > 0 and b.expiryDate <= :until order by b.expiryDate asc")
    List<StockBatch> findExpiringBy(@Param("until") LocalDate until);
}
